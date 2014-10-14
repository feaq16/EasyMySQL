package net.feaq16.easymysql;

import lombok.Getter;

public class Timming {

    @Getter
    private long startTime, endTime;
    @Getter
    private final String name;
    
    public Timming(String name) {
        this.name = name;
    }
    
    public Timming start() {
        this.startTime = System.currentTimeMillis();
        return this;
    }
    
    public Timming stop() {
        this.endTime = System.currentTimeMillis();
        return this;
    }
    
    public long getExecutingTime() {
        if(this.startTime == 0 || this.endTime == 0) {
            return 0;
        }
        
        return this.startTime - this.endTime;
    }
    
    @Override
    public String toString() {
        return this.getName() + " executing time: " + this.getExecutingTime();
    }
    
}
