package com.matchcatchup.service;

import com.matchcatchup.entity.CollectedDelivery;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

public class BroadcastStatsHelper {

    @Data
    public static class BroadcastFigures {
        private String striker = "Batter";
        private int strikerRuns = 0;
        private int strikerBalls = 0;
        private String nonStriker = "Non-Striker";
        private int nonStrikerRuns = 0;
        private int nonStrikerBalls = 0;
        private String bowler = "Bowler";
        private int bowlerWickets = 0;
        private int bowlerRuns = 0;
        private double bowlerOvers = 0.0;
    }

    public static BroadcastFigures calculateFigures(List<CollectedDelivery> allDeliveries, int innings) {
        BroadcastFigures figures = new BroadcastFigures();
        if (allDeliveries == null || allDeliveries.isEmpty()) {
            return figures;
        }

        List<CollectedDelivery> inningsDeliveries = allDeliveries.stream()
                .filter(d -> d.getInnings() == innings)
                .collect(Collectors.toList());

        if (inningsDeliveries.isEmpty()) {
            return figures;
        }

        CollectedDelivery latest = inningsDeliveries.get(inningsDeliveries.size() - 1);
        String striker = latest.getBatter() != null ? latest.getBatter() : "Batter";
        String bowler = latest.getBowler() != null ? latest.getBowler() : "Bowler";

        figures.setStriker(striker);
        figures.setBowler(bowler);

        // Striker runs & legal balls
        figures.setStrikerRuns(inningsDeliveries.stream()
                .filter(d -> striker.equalsIgnoreCase(d.getBatter()))
                .mapToInt(CollectedDelivery::getRuns)
                .sum());
        figures.setStrikerBalls((int) inningsDeliveries.stream()
                .filter(d -> striker.equalsIgnoreCase(d.getBatter()) && d.isLegalDelivery())
                .count());

        // Bowler wickets, runs, and overs
        figures.setBowlerWickets((int) inningsDeliveries.stream()
                .filter(d -> bowler.equalsIgnoreCase(d.getBowler()) && d.isWicket())
                .count());
        figures.setBowlerRuns(inningsDeliveries.stream()
                .filter(d -> bowler.equalsIgnoreCase(d.getBowler()))
                .mapToInt(CollectedDelivery::getTotalRuns)
                .sum());
        long bowlerLegalBalls = inningsDeliveries.stream()
                .filter(d -> bowler.equalsIgnoreCase(d.getBowler()) && d.isLegalDelivery())
                .count();
        double bowlerOvers = (bowlerLegalBalls / 6) + ((bowlerLegalBalls % 6) / 10.0);
        figures.setBowlerOvers(Math.round(bowlerOvers * 10.0) / 10.0);

        // Identify dismissed batters in this innings
        Set<String> outBatters = inningsDeliveries.stream()
                .filter(CollectedDelivery::isWicket)
                .map(CollectedDelivery::getBatter)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Find non-striker: most recent batter in this innings who is not the striker and not out
        String nonStriker = null;
        for (int i = inningsDeliveries.size() - 1; i >= 0; i--) {
            String b = inningsDeliveries.get(i).getBatter();
            if (b != null && !b.equalsIgnoreCase(striker) && !outBatters.contains(b.toLowerCase())) {
                nonStriker = b;
                break;
            }
        }

        // If no not-out non-striker found from deliveries, check any other batter
        if (nonStriker == null) {
            for (int i = inningsDeliveries.size() - 1; i >= 0; i--) {
                String b = inningsDeliveries.get(i).getBatter();
                if (b != null && !b.equalsIgnoreCase(striker)) {
                    nonStriker = b;
                    break;
                }
            }
        }

        if (nonStriker != null) {
            final String ns = nonStriker;
            figures.setNonStriker(ns);
            figures.setNonStrikerRuns(inningsDeliveries.stream()
                    .filter(d -> ns.equalsIgnoreCase(d.getBatter()))
                    .mapToInt(CollectedDelivery::getRuns)
                    .sum());
            figures.setNonStrikerBalls((int) inningsDeliveries.stream()
                    .filter(d -> ns.equalsIgnoreCase(d.getBatter()) && d.isLegalDelivery())
                    .count());
        }

        return figures;
    }
}
