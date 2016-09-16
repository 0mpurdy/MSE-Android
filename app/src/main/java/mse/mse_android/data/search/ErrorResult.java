package mse.mse_android.data.search;

/**
 * Created by Michael Purdy on 04/01/2016.
 */
public class ErrorResult implements IResult {

    private String error;

    public ErrorResult(String error) {
        this.error = error;
    }

    @Override
    public String getBlock() {
        return error;
    }

}
