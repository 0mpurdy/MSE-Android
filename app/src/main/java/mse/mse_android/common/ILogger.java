package mse.mse_android.common;

/**
 * Created by mj_pu_000 on 09/11/2015.
 */
public interface ILogger {

    void log(LogLevel logLevel, String message);
    void closeLog();

}