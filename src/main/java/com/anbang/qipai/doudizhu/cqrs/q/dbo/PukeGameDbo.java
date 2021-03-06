package com.anbang.qipai.doudizhu.cqrs.q.dbo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.anbang.qipai.doudizhu.cqrs.c.domain.PukeGameValueObject;
import com.anbang.qipai.doudizhu.plan.bean.PlayerInfo;
import com.dml.mpgame.game.GamePlayerValueObject;
import com.dml.mpgame.game.GameState;

public class PukeGameDbo {
	private String id;
	private int panshu;
	private int renshu;
	private boolean qxp;// 去小牌
	private boolean szfbxp;
	private int difen;
	private GameState state;// 原来是 waitingStart, playing, waitingNextPan, finished
	private int panNo;
	private List<PukeGamePlayerDbo> players;

	public PukeGameDbo() {

	}

	public PukeGameDbo(PukeGameValueObject pukeGame, Map<String, PlayerInfo> playerInfoMap) {
		id = pukeGame.getId();
		panshu = pukeGame.getPanshu();
		renshu = pukeGame.getRenshu();
		qxp = pukeGame.isQxp();
		szfbxp = pukeGame.isSzfbxp();
		difen = pukeGame.getDifen();
		state = pukeGame.getState();
		panNo = pukeGame.getPanNo();
		players = new ArrayList<>();
		Map<String, Integer> playerTotalScoreMap = pukeGame.getPlayerTotalScoreMap();
		for (GamePlayerValueObject playerValueObject : pukeGame.getPlayers()) {
			String playerId = playerValueObject.getId();
			PlayerInfo playerInfo = playerInfoMap.get(playerId);
			PukeGamePlayerDbo playerDbo = new PukeGamePlayerDbo();
			playerDbo.setHeadimgurl(playerInfo.getHeadimgurl());
			playerDbo.setNickname(playerInfo.getNickname());
			playerDbo.setGender(playerInfo.getGender());
			playerDbo.setOnlineState(playerValueObject.getOnlineState());
			playerDbo.setPlayerId(playerId);
			playerDbo.setState(playerValueObject.getState());
			if (playerTotalScoreMap.get(playerId) != null) {
				playerDbo.setTotalScore(playerTotalScoreMap.get(playerId));
			}
			players.add(playerDbo);
		}
	}

	public PukeGamePlayerDbo findPlayer(String playerId) {
		for (PukeGamePlayerDbo player : players) {
			if (player.getPlayerId().equals(playerId)) {
				return player;
			}
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getPanshu() {
		return panshu;
	}

	public void setPanshu(int panshu) {
		this.panshu = panshu;
	}

	public int getRenshu() {
		return renshu;
	}

	public void setRenshu(int renshu) {
		this.renshu = renshu;
	}

	public GameState getState() {
		return state;
	}

	public void setState(GameState state) {
		this.state = state;
	}

	public int getPanNo() {
		return panNo;
	}

	public void setPanNo(int panNo) {
		this.panNo = panNo;
	}

	public List<PukeGamePlayerDbo> getPlayers() {
		return players;
	}

	public void setPlayers(List<PukeGamePlayerDbo> players) {
		this.players = players;
	}

	public boolean isQxp() {
		return qxp;
	}

	public void setQxp(boolean qxp) {
		this.qxp = qxp;
	}

	public int getDifen() {
		return difen;
	}

	public void setDifen(int difen) {
		this.difen = difen;
	}

	public boolean isSzfbxp() {
		return szfbxp;
	}

	public void setSzfbxp(boolean szfbxp) {
		this.szfbxp = szfbxp;
	}

}
