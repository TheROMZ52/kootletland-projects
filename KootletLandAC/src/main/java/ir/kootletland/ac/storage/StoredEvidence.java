package ir.kootletland.ac.storage;

public record StoredEvidence(String playerName,String check,double confidence,double violation,long createdAtMillis,String data) {}
