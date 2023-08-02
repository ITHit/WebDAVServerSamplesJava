package com.ithit.webdav.samples.collectionsync;

import java.util.ArrayList;

import com.ithit.webdav.server.synchronization.ChangedItem;
import com.ithit.webdav.server.synchronization.Changes;

/**
 * Represents collection changes.
 */
public class DavChanges extends ArrayList<ChangedItem> implements Changes {

    private String newSyncToken;
    private boolean moreResults;

    /**
     * @see Changes#setNewSyncToken(String) ;
     */
    @Override
    public void setNewSyncToken(String newSyncToken) {
        this.newSyncToken = newSyncToken;
    }

    /**
     * @see Changes#getNewSyncToken()
     */
    @Override
    public String getNewSyncToken() {
        return newSyncToken;
    }

    /**
     * @see Changes#setMoreResults(boolean)
     */
    @Override
    public void setMoreResults(boolean moreResults) {
        this.moreResults = moreResults;
    }

    /**
     * @see Changes#getMoreResults()
     */
    @Override
    public boolean getMoreResults() {
        return moreResults;
    }
}

