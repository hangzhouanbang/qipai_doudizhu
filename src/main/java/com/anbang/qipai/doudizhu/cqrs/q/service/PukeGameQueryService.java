package com.anbang.qipai.doudizhu.cqrs.q.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anbang.qipai.doudizhu.cqrs.c.domain.PukeGameValueObject;
import com.anbang.qipai.doudizhu.cqrs.c.domain.result.DoudizhuJuResult;
import com.anbang.qipai.doudizhu.cqrs.q.dao.GameFinishVoteDboDao;
import com.anbang.qipai.doudizhu.cqrs.q.dao.JuResultDboDao;
import com.anbang.qipai.doudizhu.cqrs.q.dao.PukeGameDboDao;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.GameFinishVoteDbo;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.JuResultDbo;
import com.anbang.qipai.doudizhu.cqrs.q.dbo.PukeGameDbo;
import com.anbang.qipai.doudizhu.plan.bean.PlayerInfo;
import com.anbang.qipai.doudizhu.plan.dao.PlayerInfoDao;
import com.dml.mpgame.game.extend.vote.GameFinishVoteValueObject;

@Service
public class PukeGameQueryService {

	@Autowired
	private PukeGameDboDao pukeGameDboDao;

	@Autowired
	private PlayerInfoDao playerInfoDao;

	@Autowired
	private GameFinishVoteDboDao gameFinishVoteDboDao;

	@Autowired
	private JuResultDboDao juResultDboDao;

	public PukeGameDbo findPukeGameDboById(String gameId) {
		return pukeGameDboDao.findById(gameId);
	}

	public void newPukeGame(PukeGameValueObject pukeGame) {
		Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
		pukeGame.allPlayerIds().forEach((playerId) -> playerInfoMap.put(playerId, playerInfoDao.findById(playerId)));
		PukeGameDbo pukeGameDbo = new PukeGameDbo(pukeGame, playerInfoMap);
		pukeGameDboDao.save(pukeGameDbo);
	}

	public void joinGame(PukeGameValueObject pukeGame) {
		Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
		pukeGame.allPlayerIds().forEach((playerId) -> playerInfoMap.put(playerId, playerInfoDao.findById(playerId)));
		PukeGameDbo pukeGameDbo = new PukeGameDbo(pukeGame, playerInfoMap);
		pukeGameDboDao.save(pukeGameDbo);
	}

	public void leaveGame(PukeGameValueObject pukeGame) {
		Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
		pukeGame.allPlayerIds().forEach((playerId) -> playerInfoMap.put(playerId, playerInfoDao.findById(playerId)));
		PukeGameDbo pukeGameDbo = new PukeGameDbo(pukeGame, playerInfoMap);
		pukeGameDboDao.save(pukeGameDbo);

		GameFinishVoteValueObject gameFinishVoteValueObject = pukeGame.getVote();
		if (gameFinishVoteValueObject != null) {
			gameFinishVoteDboDao.removeGameFinishVoteDboByGameId(pukeGame.getId());
			GameFinishVoteDbo gameFinishVoteDbo = new GameFinishVoteDbo();
			gameFinishVoteDbo.setVote(gameFinishVoteValueObject);
			gameFinishVoteDbo.setGameId(pukeGame.getId());
			gameFinishVoteDboDao.save(gameFinishVoteDbo);
		}
	}

	public void backToGame(String playerId, PukeGameValueObject pukeGameValueObject) {
		pukeGameDboDao.updatePlayerOnlineState(pukeGameValueObject.getId(), playerId,
				pukeGameValueObject.findPlayerOnlineState(playerId));
		GameFinishVoteValueObject gameFinishVoteValueObject = pukeGameValueObject.getVote();
		if (gameFinishVoteValueObject != null) {
			gameFinishVoteDboDao.update(pukeGameValueObject.getId(), gameFinishVoteValueObject);
		}
	}

	public void finish(PukeGameValueObject pukeGameValueObject) {
		gameFinishVoteDboDao.removeGameFinishVoteDboByGameId(pukeGameValueObject.getId());
		GameFinishVoteValueObject gameFinishVoteValueObject = pukeGameValueObject.getVote();
		GameFinishVoteDbo gameFinishVoteDbo = new GameFinishVoteDbo();
		gameFinishVoteDbo.setVote(gameFinishVoteValueObject);
		gameFinishVoteDbo.setGameId(pukeGameValueObject.getId());
		gameFinishVoteDboDao.save(gameFinishVoteDbo);

		Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
		pukeGameValueObject.allPlayerIds()
				.forEach((playerId) -> playerInfoMap.put(playerId, playerInfoDao.findById(playerId)));
		PukeGameDbo pukeGameDbo = new PukeGameDbo(pukeGameValueObject, playerInfoMap);
		pukeGameDboDao.save(pukeGameDbo);

		if (pukeGameValueObject.getJuResult() != null) {
			DoudizhuJuResult doudizhuJuResult = (DoudizhuJuResult) pukeGameValueObject.getJuResult();
			JuResultDbo juResultDbo = new JuResultDbo(pukeGameValueObject.getId(), null, doudizhuJuResult);
			juResultDboDao.save(juResultDbo);
		}
	}

	public void voteToFinish(PukeGameValueObject pukeGameValueObject) {
		GameFinishVoteValueObject gameFinishVoteValueObject = pukeGameValueObject.getVote();
		gameFinishVoteDboDao.update(pukeGameValueObject.getId(), gameFinishVoteValueObject);

		Map<String, PlayerInfo> playerInfoMap = new HashMap<>();
		pukeGameValueObject.allPlayerIds()
				.forEach((playerId) -> playerInfoMap.put(playerId, playerInfoDao.findById(playerId)));
		PukeGameDbo pukeGameDbo = new PukeGameDbo(pukeGameValueObject, playerInfoMap);
		pukeGameDboDao.save(pukeGameDbo);

		if (pukeGameValueObject.getJuResult() != null) {
			DoudizhuJuResult doudizhuJuResult = (DoudizhuJuResult) pukeGameValueObject.getJuResult();
			JuResultDbo juResultDbo = new JuResultDbo(pukeGameValueObject.getId(), null, doudizhuJuResult);
			juResultDboDao.save(juResultDbo);
		}
	}

	public GameFinishVoteDbo findGameFinishVoteDbo(String gameId) {
		return gameFinishVoteDboDao.findByGameId(gameId);
	}

}
