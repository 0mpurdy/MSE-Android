package mse.mse_android.search;

import android.content.res.AssetManager;

import mse.mse_android.common.*;
import mse.mse_android.data.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mj_pu_000 on 10/11/2015.
 */
public class IndexStore {

    Config cfg;

    HashMap<String, AuthorIndex> authorIndexes;

    public IndexStore(Config cfg) {
        this.cfg = cfg;
        authorIndexes = new HashMap<>();
    }

    public AuthorIndex getIndex(Author author, ILogger logger, AssetManager assetManager) {

        AuthorIndex authorIndex = authorIndexes.get(author.getCode());

        if (authorIndex == null) {
            authorIndex = new AuthorIndex(author, logger);
            authorIndex.loadIndex(assetManager);
            authorIndexes.put(author.getCode(), authorIndex);
        }

        return authorIndex;
    }

}
