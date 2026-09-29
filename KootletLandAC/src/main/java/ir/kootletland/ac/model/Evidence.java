package ir.kootletland.ac.model;

import java.time.Instant;
import java.util.Map;

public record Evidence(String player,String check,double confidence,double violation,Instant timestamp,Map<String,Object> data) {}
