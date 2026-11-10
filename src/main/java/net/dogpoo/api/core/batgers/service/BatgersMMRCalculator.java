package net.dogpoo.api.core.batgers.service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import net.dogpoo.api.core.batgers.model.BatgersPlayerMMRModel;

public final class BatgersMMRCalculator {

	public static List<BatgersPlayerMMRModel> calculateMMR(List<BatgersPlayerMMRModel> players) {

		int baseLoseScore = (players.size() - 1) * 10;
		List<BatgersPlayerMMRModel> winners = players.stream()
				.filter(BatgersPlayerMMRModel::isWinner)
				.collect(Collectors.toList());

		List<BatgersPlayerMMRModel> losers = players.stream()
				.filter(p -> !p.isWinner())
				.collect(Collectors.toList());

		if (winners.isEmpty() || losers.isEmpty()) {
			return players;
		}

		int totalPool = 0;
		// 패배자 점수 차감
		for (BatgersPlayerMMRModel loser : losers) {
			loser.setMmrDelta(-baseLoseScore);
			totalPool += baseLoseScore;
		}

		// 승자 평균 딜량
		double avgDamage = winners.stream().mapToDouble(BatgersPlayerMMRModel::getDamage).average().orElse(1);
		HashMap<BatgersPlayerMMRModel, Double> weightMap = new HashMap<BatgersPlayerMMRModel, Double>();

		double totalWeight = 0;
		for (BatgersPlayerMMRModel p : winners) {
			double damageRatio = p.getDamage() / avgDamage;
			double expectedDamage = 500 + ((p.getMmr() - 1000) * 0.8);
			expectedDamage = Math.max(300, expectedDamage);
			double performance = p.getDamage() / expectedDamage;
			double weight = (damageRatio * 0.6) + (performance * 0.4);
			weight = Math.max(0.3, weight);

			weightMap.put(p, weight);
			totalWeight += weight;
		}

		// 점수 분배
		for (BatgersPlayerMMRModel p : winners) {
			double weight = weightMap.get(p);
			double gain = (double) Math.round((totalPool * (weight / totalWeight)) * 10000.0) / 10000.0;
			p.setMmrDelta(gain);
		}

		// 최종 mmr
		for (BatgersPlayerMMRModel p : players) {
			p.setNewMmr(p.getMmr() + p.getMmrDelta());
		}

		return players;
	}
}