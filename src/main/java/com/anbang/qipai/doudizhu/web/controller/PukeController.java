package com.anbang.qipai.doudizhu.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.anbang.qipai.doudizhu.cqrs.c.domain.result.PukeActionResult;
import com.anbang.qipai.doudizhu.cqrs.c.domain.result.QiangdizhuResult;
import com.anbang.qipai.doudizhu.cqrs.c.domain.result.ReadyToNextPanResult;
import com.anbang.qipai.doudizhu.cqrs.c.domain.state.Qiangdizhu;
import com.anbang.qipai.doudizhu.cqrs.c.service.GameCmdService;
import com.anbang.qipai.doudizhu.cqrs.c.service.PlayerAuthService;
import com.anbang.qipai.doudizhu.cqrs.c.service.PukePlayCmdService;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.GameLatestInfoDbo;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.JuResultDbo;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.PanResultDbo;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.PukeGameDbo;
import com.anbang.qipai.doudizhu.cqrs.q.service.PukeGameQueryService;
import com.anbang.qipai.doudizhu.cqrs.q.service.PukePlayQueryService;
import com.anbang.qipai.doudizhu.msg.msjobj.PukeHistoricalJuResult;
import com.anbang.qipai.doudizhu.msg.msjobj.PukeHistoricalPanResult;
import com.anbang.qipai.doudizhu.msg.service.DoudizhuGameMsgService;
import com.anbang.qipai.doudizhu.msg.service.DoudizhuResultMsgService;
import com.anbang.qipai.doudizhu.web.vo.CommonVO;
import com.anbang.qipai.doudizhu.web.vo.JuResultVO;
import com.anbang.qipai.doudizhu.web.vo.PanActionFrameVO;
import com.anbang.qipai.doudizhu.web.vo.PanResultVO;
import com.anbang.qipai.doudizhu.websocket.GamePlayWsNotifier;
import com.anbang.qipai.doudizhu.websocket.QueryScope;
import com.anbang.qipai.doudizhu.websocket.WatchQueryScope;
import com.dml.doudizhu.pan.PanActionFrame;
import com.dml.mpgame.game.player.GamePlayerOnlineState;

@RestController
@RequestMapping("/pk")
public class PukeController {

	@Autowired
	private PlayerAuthService playerAuthService;

	@Autowired
	private PukePlayCmdService pukePlayCmdService;

	@Autowired
	private PukePlayQueryService pukePlayQueryService;

	@Autowired
	private PukeGameQueryService pukeGameQueryService;

	@Autowired
	private DoudizhuResultMsgService doudizhuResultMsgService;

	@Autowired
	private DoudizhuGameMsgService gameMsgService;

	@Autowired
	private GamePlayWsNotifier wsNotifier;

	@Autowired
	private GameCmdService gameCmdService;

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * 当前盘我应该看到的所有信息
	 */
	@RequestMapping(value = "/pan_action_frame_for_me")
	@ResponseBody
	public CommonVO panactionframeforme(String token, String gameId) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		PanActionFrame panActionFrame;
		GameLatestInfoDbo gameLatestInfoDbo;
		try {
			panActionFrame = pukePlayQueryService.findAndFilterCurrentPanValueObjectForPlayer(gameId, playerId);
			gameLatestInfoDbo = pukePlayQueryService.findGameLatestInfoDboById(gameId);
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getMessage());
			return vo;
		}
		data.put("panActionFrame", new PanActionFrameVO(panActionFrame, gameLatestInfoDbo));
		return vo;
	}

	/**
	 * @param gameId
	 * @param panNo
	 *            0代表不知道盘号，那么就取最新的一盘
	 * @return
	 */
	@RequestMapping(value = "/pan_result")
	@ResponseBody
	public CommonVO panresult(String gameId, int panNo) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		PukeGameDbo pukeGameDbo = pukeGameQueryService.findPukeGameDboById(gameId);
		if (panNo == 0) {
			panNo = pukeGameDbo.getPanNo();
		}
		PanResultDbo panResultDbo = pukePlayQueryService.findPanResultDbo(gameId, panNo);
		data.put("panResult", new PanResultVO(panResultDbo, pukeGameDbo));
		return vo;
	}

	@RequestMapping(value = "/ju_result")
	@ResponseBody
	public CommonVO juresult(String gameId) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		PukeGameDbo pukeGameDbo = pukeGameQueryService.findPukeGameDboById(gameId);
		JuResultDbo juResultDbo = pukePlayQueryService.findJuResultDbo(gameId);
		data.put("juResult", new JuResultVO(juResultDbo, pukeGameDbo));
		return vo;
	}

	@RequestMapping(value = "/qiangdizhu")
	@ResponseBody
	public CommonVO qiangdizhu(String token, boolean qiang) {
		CommonVO vo = new CommonVO();
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}
		Map data = new HashMap();
		List<String> queryScopes = new ArrayList<>();
		data.put("queryScopes", queryScopes);
		vo.setData(data);
		QiangdizhuResult qiangdizhuResult;
		try {
			qiangdizhuResult = pukePlayCmdService.qiangdizhu(playerId, qiang, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		pukePlayQueryService.qiangdizhu(qiangdizhuResult);
		// 通知其他人
		for (String otherPlayerId : qiangdizhuResult.getPukeGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = qiangdizhuResult.getPukeGame().findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					wsNotifier.notifyToQuery(otherPlayerId,
							QueryScope.scopesForState(qiangdizhuResult.getPukeGame().getState(),
									qiangdizhuResult.getPukeGame().findPlayerState(otherPlayerId)));
				}
			}
		}
		queryScopes.add(QueryScope.panForMe.name());
		return vo;
	}

	@RequestMapping(value = "/da")
	@ResponseBody
	public CommonVO da(String token, @RequestBody List<Integer> paiIds, String dianshuZuheIdx) {
		long startTime = System.currentTimeMillis();
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		List<String> queryScopes = new ArrayList<>();
		data.put("queryScopes", queryScopes);
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			long endTime = System.currentTimeMillis();
			logger.info("action:da," + "startTime:" + startTime + "," + "playerId:" + playerId + "," + "paiIds:"
					+ paiIds + "," + "dianshuZuheIdx:" + dianshuZuheIdx + "," + "success:" + vo.isSuccess() + ",msg:"
					+ vo.getMsg() + "," + "endTime:" + endTime + "," + "use:" + (endTime - startTime) + "ms");
			return vo;
		}

		PukeActionResult pukeActionResult;
		String endFlag = "query";
		try {
			pukeActionResult = pukePlayCmdService.da(playerId, new ArrayList<>(paiIds), dianshuZuheIdx,
					System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			long endTime = System.currentTimeMillis();
			logger.info("action:da," + "startTime:" + startTime + "," + "playerId:" + playerId + "," + "paiIds:"
					+ paiIds + "," + "dianshuZuheIdx:" + dianshuZuheIdx + "," + "success:" + vo.isSuccess() + ",msg:"
					+ vo.getMsg() + "," + "endTime:" + endTime + "," + "use:" + (endTime - startTime) + "ms");
			return vo;
		}
		pukePlayQueryService.action(pukeActionResult);

		if (pukeActionResult.getPanResult() == null) {// 盘没结束
			queryScopes.add(QueryScope.gameInfo.name());
			queryScopes.add(QueryScope.panForMe.name());
		} else {// 盘结束了
			String gameId = pukeActionResult.getPukeGame().getId();
			PukeGameDbo pukeGameDbo = pukeGameQueryService.findPukeGameDboById(gameId);
			if (pukeActionResult.getJuResult() != null) {// 局也结束了
				JuResultDbo juResultDbo = pukePlayQueryService.findJuResultDbo(gameId);
				PukeHistoricalJuResult juResult = new PukeHistoricalJuResult(juResultDbo, pukeGameDbo);
				doudizhuResultMsgService.recordJuResult(juResult);

				gameMsgService.gameFinished(gameId);
				queryScopes.add(QueryScope.juResult.name());
				endFlag = WatchQueryScope.watchEnd.name();
			} else {
				queryScopes.add(QueryScope.gameInfo.name());
				queryScopes.add(QueryScope.panResult.name());
				endFlag = WatchQueryScope.panResult.name();
			}
			PanResultDbo panResultDbo = pukePlayQueryService.findPanResultDbo(gameId,
					pukeActionResult.getPanResult().getPan().getNo());
			PukeHistoricalPanResult panResult = new PukeHistoricalPanResult(panResultDbo, pukeGameDbo);
			doudizhuResultMsgService.recordPanResult(panResult);
			gameMsgService.panFinished(pukeActionResult.getPukeGame(),
					pukeActionResult.getPanActionFrame().getPanAfterAction());
		}
		// 通知其他人
		for (String otherPlayerId : pukeActionResult.getPukeGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = pukeActionResult.getPukeGame().findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					wsNotifier.notifyToQuery(otherPlayerId,
							QueryScope.scopesForState(pukeActionResult.getPukeGame().getState(),
									pukeActionResult.getPukeGame().findPlayerState(otherPlayerId)));
				}
			}
		}

		// 通知观战者
		hintWatcher(pukeActionResult.getPukeGame().getId(), endFlag);

		long endTime = System.currentTimeMillis();
		logger.info("action:da," + "startTime:" + startTime + "," + "gameId:" + pukeActionResult.getPukeGame().getId()
				+ "," + "playerId:" + playerId + "," + "paiIds:" + paiIds + "," + "dianshuZuheIdx:" + dianshuZuheIdx
				+ "," + "success:" + vo.isSuccess() + ",msg:" + vo.getMsg() + "," + "endTime:" + endTime + "," + "use:"
				+ (endTime - startTime) + "ms");
		return vo;
	}

	@RequestMapping(value = "/guo")
	@ResponseBody
	public CommonVO guo(String token) {
		long startTime = System.currentTimeMillis();
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		List<String> queryScopes = new ArrayList<>();
		data.put("queryScopes", queryScopes);
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			long endTime = System.currentTimeMillis();
			logger.info("action:guo," + "startTime:" + startTime + "," + "playerId:" + playerId + "," + "success:"
					+ vo.isSuccess() + ",msg:" + vo.getMsg() + "," + "endTime:" + endTime + "," + "use:"
					+ (endTime - startTime) + "ms");
			return vo;
		}

		PukeActionResult pukeActionResult;
		try {
			pukeActionResult = pukePlayCmdService.guo(playerId, System.currentTimeMillis());
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			long endTime = System.currentTimeMillis();
			logger.info("action:guo," + "startTime:" + startTime + "," + "playerId:" + playerId + "," + "success:"
					+ vo.isSuccess() + ",msg:" + vo.getMsg() + "," + "endTime:" + endTime + "," + "use:"
					+ (endTime - startTime) + "ms");
			return vo;
		}
		pukePlayQueryService.action(pukeActionResult);

		queryScopes.add(QueryScope.panForMe.name());
		queryScopes.add(QueryScope.gameInfo.name());

		// 通知其他人
		for (String otherPlayerId : pukeActionResult.getPukeGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = pukeActionResult.getPukeGame().findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					wsNotifier.notifyToQuery(otherPlayerId,
							QueryScope.scopesForState(pukeActionResult.getPukeGame().getState(),
									pukeActionResult.getPukeGame().findPlayerState(otherPlayerId)));
				}
			}
		}

		// 通知观战者
		hintWatcher(pukeActionResult.getPukeGame().getId(), "query");

		long endTime = System.currentTimeMillis();
		logger.info("action:guo," + "startTime:" + startTime + "," + "gameId:" + pukeActionResult.getPukeGame().getId()
				+ "," + "playerId:" + playerId + "," + "success:" + vo.isSuccess() + ",msg:" + vo.getMsg() + ","
				+ "endTime:" + endTime + "," + "use:" + (endTime - startTime) + "ms");
		return vo;
	}

	@RequestMapping(value = "/ready_to_next_pan")
	@ResponseBody
	public CommonVO readytonextpan(String token) {
		CommonVO vo = new CommonVO();
		Map data = new HashMap();
		vo.setData(data);
		String playerId = playerAuthService.getPlayerIdByToken(token);
		if (playerId == null) {
			vo.setSuccess(false);
			vo.setMsg("invalid token");
			return vo;
		}

		ReadyToNextPanResult readyToNextPanResult;
		try {
			readyToNextPanResult = pukePlayCmdService.readyToNextPan(playerId);
		} catch (Exception e) {
			vo.setSuccess(false);
			vo.setMsg(e.getClass().getName());
			return vo;
		}
		pukePlayQueryService.readyToNextPan(readyToNextPanResult);

		// 通知其他人
		for (String otherPlayerId : readyToNextPanResult.getPukeGame().allPlayerIds()) {
			if (!otherPlayerId.equals(playerId)) {
				GamePlayerOnlineState onlineState = readyToNextPanResult.getPukeGame()
						.findPlayerOnlineState(otherPlayerId);
				if (onlineState.equals(GamePlayerOnlineState.online)) {
					List<QueryScope> scopes = QueryScope.scopesForState(readyToNextPanResult.getPukeGame().getState(),
							readyToNextPanResult.getPukeGame().findPlayerState(otherPlayerId));
					scopes.remove(QueryScope.panResult);
					wsNotifier.notifyToQuery(otherPlayerId, scopes);
				}
			}
		}

		List<QueryScope> queryScopes = new ArrayList<>();
		queryScopes.add(QueryScope.gameInfo);
		if (readyToNextPanResult.getPukeGame().getState().name().equals(Qiangdizhu.name)) {
			queryScopes.add(QueryScope.panForMe);
		}
		data.put("queryScopes", queryScopes);
		return vo;
	}

	/**
	 * 通知观战者
	 */
	private void hintWatcher(String gameId, String flag) {
		Map<String, Object> map = gameCmdService.getwatch(gameId);
		if (!CollectionUtils.isEmpty(map)) {
			List<String> playerIds = map.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList());
			wsNotifier.notifyToWatchQuery(playerIds, flag);
			if (WatchQueryScope.watchEnd.name().equals(flag)) {
				gameCmdService.recycleWatch(gameId);
			}
		}
	}

}
