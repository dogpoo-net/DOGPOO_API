package net.dogpoo.api.core.batgers.model;

public class BatgersPlayerMMRModel {
	private int playerNo;
    private String name;
    private double mmr;
    private double damage;
    private boolean winner;
    private double mmrDelta;
    private double newMmr;
    public BatgersPlayerMMRModel(int playerNo, String name, double mmr, double damage, boolean winner) {
    	this.playerNo = playerNo;
        this.name = name;
        this.mmr = mmr;
        this.damage = damage;
        this.winner = winner;
    }
    public int getPlayerNo() {
		return playerNo;
	}
	public String getName() {
        return name;
    }
    public double getMmr() {
        return mmr;
    }
    public double getDamage() {
        return damage;
    }
    public boolean isWinner() {
        return winner;
    }
    public double getMmrDelta() {
        return mmrDelta;
    }
    public void setMmrDelta(double mmrDelta) {
        this.mmrDelta = mmrDelta;
    }
    public double getNewMmr() {
        return newMmr;
    }
    public void setNewMmr(double newMmr) {
        this.newMmr = newMmr;
    }
	@Override
	public String toString() {
		return "BatgersPlayerMMRModel [playerNo=" + playerNo + ", name=" + name + ", mmr=" + mmr + ", damage=" + damage
				+ ", winner=" + winner + ", mmrDelta=" + mmrDelta + ", newMmr=" + newMmr + "]";
	}
}