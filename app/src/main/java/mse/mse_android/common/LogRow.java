package mse.mse_android.common;

/**
 * Created by mj_pu_000 on 18/11/2015.
 */
public class LogRow {

    public LogLevel logLevel;
    public String message;

    public LogRow(LogLevel logLevel, String message){
        this.logLevel = logLevel;
        this.message = message;
    }

}