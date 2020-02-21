package com.example.axus.temiapptest.Model;
import java.util.HashMap;
import java.util.Map;

/**
 * This class should be modified to input the MapID for each map.
 */
public class MapPointHelper {
    public HashMap<String, String> mapNameIdHashMap;
    public HashMap<Integer, String> mapNameByLevelHashmap;

    public void initialiseMapID() {
        mapNameIdHashMap = new HashMap<>();
        mapNameIdHashMap.put("0653870a-4f78-4ec2-b555-300fe02002ff", "SIT7");
        mapNameIdHashMap.put("b19513e8-bc9b-4edb-bdf0-4416088d3dd4", "SIT6");
        mapNameIdHashMap.put("e8d7ca3b-2e2f-410d-bd2b-85758313cc03", "5ggarage");
        mapNameIdHashMap.put("58392799-5dac-44f3-8630-0c28ab81e111", "MoccaRoom");
        mapNameIdHashMap.put("54e4b970-ff54-4c17-a021-11b307d62d5e", "CodeX");
        mapNameIdHashMap.put("5c899c07-7b0a-4f1c-810e-f4bb419e1547", "A1Map");
        //mapNameIdHashMap.put("3", "SIT1F");

        // map level to mapname
        mapNameByLevelHashmap = new HashMap<>();
        mapNameByLevelHashmap.put(7,"SIT7");
        mapNameByLevelHashmap.put(6,"SIT6");
        //mapNameByLevelHashmap.put(3,"MoccaRoom");
        //mapNameByLevelHashmap.put(3,"CodeX");
        mapNameByLevelHashmap.put(3,"5ggarage");
        mapNameByLevelHashmap.put(4,"A1Map");


    }

    public String getMapVersionIdFromMapName(String currentMap)
    {
        for (Map.Entry<String, String> entry : mapNameIdHashMap.entrySet()) {
            if(entry.getValue().equals(currentMap))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getLevelByMapName(String currentMap)
    {
        for (Map.Entry<Integer, String> entry : mapNameByLevelHashmap.entrySet()) {
            if(entry.getValue().equals(currentMap))
            {
                return entry.getKey();
            }
        }
        return 0;
    }


}
