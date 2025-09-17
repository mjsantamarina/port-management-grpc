package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.models.Crane;

import java.util.concurrent.ConcurrentHashMap;

public class CraneRepository {
    ConcurrentHashMap<Integer, Crane> craneMap;

    public CraneRepository() {
        craneMap = new ConcurrentHashMap<>();
    }

    public void addCrane(Crane crane) {
        craneMap.put(crane.getId(), crane);
    }

    public Crane getCrane(int id) {
        return craneMap.get(id);
    }


    public int getNextCraneNumber(){
        return craneMap.size() + 1;
    }


}
