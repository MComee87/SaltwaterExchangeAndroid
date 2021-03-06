package com.boonex.oo.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.boonex.oo.Connector;
import com.boonex.oo.Main;

public class SearchResultsLocationActivity extends SearchResultsBaseActivity {
	private static final int ACTIVITY_SEARCH_RESULTS=0;	
	private static final String TAG = "SearchResultsKeywordActivity";
    protected String m_sCountryCode;
    protected String m_sCity;
    protected Boolean m_isOnlineOnly;
    protected Boolean m_isWithPhotosOnly;
    protected Integer m_iStart; 
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent i = getIntent();        
        m_sCountryCode = i.getStringExtra("country");
        m_sCity = i.getStringExtra("city");
        m_isOnlineOnly = i.getBooleanExtra("online_only", false);
        m_isWithPhotosOnly = i.getBooleanExtra("with_photos_only", false);
        m_iStart = i.getIntExtra("start", 0);
        
        Log.d(TAG, "Country: " + m_sCountryCode);
        Log.d(TAG, "City: " + m_sCity);
        Log.d(TAG, "OnlineOnly: " + m_isOnlineOnly);
        Log.d(TAG, "WithPhotosOnly: " + m_isWithPhotosOnly);
        Log.d(TAG, "Start: " + m_iStart);
        
        reloadRemoteData ();
    }
    
    protected void reloadRemoteData () {
        Connector oConnector = Main.getConnector();
        
        Object[] aParams = {
        		oConnector.getUsername(), 
        		oConnector.getPassword(),
        		Main.getLang(),
        		m_sCountryCode,
        		m_sCity,
        		m_isOnlineOnly ? "1" : "0",
        		m_isWithPhotosOnly ? "1" : "0",
        		String.format("%d", m_iStart),
        		String.format("%d", m_iPerPage)
        };                                    
        
        oConnector.execAsyncMethod("dolphin.getSearchResultsLocation", aParams, new Connector.Callback() {
			
			public void callFinished(Object result) {				 
				Log.d(TAG, "dolphin.getSearchResultsLocation result: " + result.toString());
				
				m_aProfiles = (Object [])result;
				
				Log.d(TAG, "dolphin.getSearchResultsLocation num: " + m_aProfiles.length); 
								
				checkNextButton(m_aProfiles.length == m_iPerPage);
				
				adapterSearchResults = new SearchResultsAdapter(m_actThis, m_aProfiles, m_aProfiles.length == m_iPerPage);
		        setListAdapter(adapterSearchResults);
			}
        }, this);    	
    }
    
	public void onNext () {		
		Intent i = new Intent(this, SearchResultsLocationActivity.class);
		i.putExtra("country", m_sCountryCode);
		i.putExtra("city", m_sCity);
		i.putExtra("online_only", m_isOnlineOnly);
		i.putExtra("with_photos_only", m_isWithPhotosOnly);
		i.putExtra("start", m_iStart + m_iPerPage);
		startActivityForResult(i, ACTIVITY_SEARCH_RESULTS);
		
	}    
    
}
