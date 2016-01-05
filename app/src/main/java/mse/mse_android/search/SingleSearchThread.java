package mse.mse_android.search;

import java.util.ArrayList;

import mse.mse_android.common.LogRow;
import mse.mse_android.data.IResult;

/**
 * Created by Michael on 17/11/2015.
 */
public abstract class SingleSearchThread extends Thread {

    abstract ArrayList<LogRow> getLog();

    abstract ArrayList<IResult> getResults();

    abstract int getNumberOfResults();

}
