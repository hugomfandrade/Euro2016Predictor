package org.hugoandrade.euro2016.predictor;

import android.support.v4.util.Pair;
import android.util.Log;
import android.util.SparseArray;

import org.hugoandrade.euro2016.predictor.data.LeagueWrapper;
import org.hugoandrade.euro2016.predictor.data.raw.Country;
import org.hugoandrade.euro2016.predictor.data.raw.LeagueUser;
import org.hugoandrade.euro2016.predictor.data.raw.Match;
import org.hugoandrade.euro2016.predictor.data.raw.Prediction;
import org.hugoandrade.euro2016.predictor.data.raw.SystemData;
import org.hugoandrade.euro2016.predictor.data.raw.User;
import org.hugoandrade.euro2016.predictor.utils.MatchUtils;
import org.hugoandrade.euro2016.predictor.utils.StaticVariableUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GlobalData {

    private static final String TAG = GlobalData.class.getSimpleName();

    private static GlobalData mInstance = null;

    public User user;
    public SystemData systemData;

    private HashSet<OnMatchesChangedListener> mOnMatchesChangedListenerSet = new HashSet<>();
    private HashSet<OnCountriesChangedListener> mOnCountriesChangedListenerSet = new HashSet<>();
    private HashSet<OnPredictionsChangedListener> mOnPredictionsChangedListenerSet = new HashSet<>();
    private HashSet<OnLatestPerformanceChangedListener> mOnLatestPerformanceChangedListenerSet = new HashSet<>();
    private HashSet<OnLeaguesChangedListener> mOnLeaguesChangedListenerSet = new HashSet<>();

    private List<Country> mCountryList = new ArrayList<>();
    private List<Match> mMatchList = new ArrayList<>();
    private List<Prediction> mPredictionList = new ArrayList<>();
    private List<LeagueWrapper> mLeagueWrapperList = new ArrayList<>();
    private HashMap<String, List<Prediction>> mLatestPerformanceMap = new HashMap<>();

    // UserID, list of matches whose predictions where fetched
    private Map<String, List<Integer>> mPredictionOfUserMap = new HashMap<>();
    // UserID, Map MatchNumber - predictions
    private HashMap<String, SparseArray<Prediction>> mMatchPredictionMap = new HashMap<>();

    /*public static GlobalData getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException(TAG + " is not initialized");
        }
        return mInstance;
    }/**/

    public static GlobalData getInstance() {
        if (mInstance == null) {
            mInstance = new GlobalData();
        }
        return mInstance;
    }

    public static void unInitialize() {
        if (mInstance == null) {
            return;
        }
        try {
            mInstance.user = null;
        } catch (IllegalStateException e) {
            Log.e(TAG, "unInitialize error: " + e.getMessage());
        }
    }
    public Calendar getServerTime() {
        return systemData.getDate();
    }

    public void setSystemData(SystemData systemData) {
        this.systemData = systemData;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setLeagues(List<LeagueWrapper> leagueWrapperList) {
        mLeagueWrapperList = leagueWrapperList;
        if (mLeagueWrapperList == null)
            mLeagueWrapperList = new ArrayList<>();
        //mLeagueWrapperList.add(LeagueWrapper.createOverall(mUserList));

        for (OnLeaguesChangedListener listener : mOnLeaguesChangedListenerSet) {
            listener.onLeaguesChanged();
        }
    }

    public List<LeagueWrapper> getLeagues() {
        return mLeagueWrapperList;
    }

    public List<Prediction> getPredictionList() {
        return mPredictionList;
    }

    public void setPredictionList(List<Prediction> predictionList) {
        this.mPredictionList = predictionList;

        for (OnPredictionsChangedListener listener : mOnPredictionsChangedListenerSet) {
            listener.onPredictionsChanged();
        }
    }

    public List<Country> getCountryList() {
        return mCountryList;
    }

    public void setCountryList(List<Country> countryList) {
        this.mCountryList = countryList;

        for (OnCountriesChangedListener listener : mOnCountriesChangedListenerSet) {
            listener.onCountriesChanged();
        }
    }

    public List<Match> getMatchList() {
        return mMatchList;
    }

    public void setMatchList(List<Match> matchList) {
        this.mMatchList = matchList;

        for (OnMatchesChangedListener listener : mOnMatchesChangedListenerSet) {
            listener.onMatchesChanged();
        }
    }

    public void setLatestPerformanceOfUsers(List<Prediction> predictionList) {

        for (Prediction p : predictionList) {
            String userID = p.getUserID();

            if (mLatestPerformanceMap.containsKey(userID)) {
                mLatestPerformanceMap.get(userID).add(p);
            } else {
                mLatestPerformanceMap.put(userID, new ArrayList<Prediction>());
                mLatestPerformanceMap.get(userID).add(p);
            }
        }

        for (OnLatestPerformanceChangedListener listener : mOnLatestPerformanceChangedListenerSet) {
            listener.onLatestPerformanceChanged();
        }
    }

    public int[] getLatestPerformance(User user) {
        List<Prediction> latestPerformancePredictionList = mLatestPerformanceMap.get(user.getID());

        if (latestPerformancePredictionList == null) {
            latestPerformancePredictionList = new ArrayList<>();
        }

        int finalMatchNumber = MatchUtils.getMatchNumberOfFirstNotPlayedMatched(mMatchList, getServerTime().getTime());

        // 2
        // 7
        int initMatchNumber = finalMatchNumber < 6 ? 1 : finalMatchNumber - 5;

        int[] a = new int[finalMatchNumber - initMatchNumber];

        int i = 0;
        for (int matchNumber = finalMatchNumber - 1 ; matchNumber >= initMatchNumber; matchNumber--) {
            int score = systemData.getRules().getRuleIncorrectPrediction();

            //Prediction prediction = null;
            for (Prediction p : latestPerformancePredictionList) {
                if (p.getMatchNumber() == matchNumber && p.getScore() != -1) {
                    score = p.getScore();
                }
            }
            a[i] = score;
            i++;
        }

        return a;
    }

    public Country getCountry(Country country) {
        if (country == null) return null;
        for (Country c : mCountryList) {
            if (c.getID().equals(country.getID())) {
                return c;
            }
        }
        return null;
    }

    public Match getMatch(int matchNumber) {
        for (Match m : mMatchList) {
            if (m.getMatchNumber() == matchNumber) {
                return m;
            }
        }
        return null;
    }

    public List<Match> getMatchList(Country country) {
        if (country == null || country.getName() == null) return new ArrayList<>();
        List<Match> matchList = new ArrayList<>();
        for (Match m : mMatchList) {
            if (country.getName().equals(m.getHomeTeamName())) {
                matchList.add(m);
            }
            if (country.getName().equals(m.getAwayTeamName())) {
                matchList.add(m);
            }
        }
        return matchList;
    }

    public List<Match> getMatchList(StaticVariableUtils.SStage stage) {
        return MatchUtils.getMatchList(mMatchList, stage);
    }

    public List<Match> getMatchList(StaticVariableUtils.SStage stage, int matchday) {
        return MatchUtils.getMatchList(mMatchList, stage, matchday);
    }

    public List<Country> getCountryList(Country country) {
        if (country == null || country.getGroup() == null) return new ArrayList<>();
        List<Country> countryList = new ArrayList<>();
        for (Country c : mCountryList) {
            if (country.getGroup().equals(c.getGroup())) {
                countryList.add(c);
            }
        }

        Collections.sort(countryList, new Comparator<Country>() {
            @Override
            public int compare(Country o1, Country o2) {
                return o1.getPosition() - o2.getPosition();
            }
        });
        return countryList;
    }

    public void setPredictionsOfUsers(int matchNumber, List<User> userList, List<Prediction> predictionList) {
        for (User u : userList) {
            String userID = u.getID();

            if (mPredictionOfUserMap.containsKey(userID)) {
                mPredictionOfUserMap.get(userID).add(matchNumber);
            } else {
                mPredictionOfUserMap.put(userID, new ArrayList<Integer>());
                mPredictionOfUserMap.get(userID).add(matchNumber);
            }

            Prediction prediction = null;
            for (Prediction p : predictionList) {
                if (p.getMatchNumber() == matchNumber && userID.equals(p.getUserID())) {
                    prediction = p;
                }
            }
            if (prediction == null) {
                prediction = Prediction.emptyInstance(matchNumber, userID);
            }

            if (mMatchPredictionMap.containsKey(userID)) {
                mMatchPredictionMap.get(userID).put(matchNumber, prediction);
            } else {
                mMatchPredictionMap.put(userID, new SparseArray<Prediction>());
                mMatchPredictionMap.get(userID).put(matchNumber, prediction);
            }
        }
    }

    public void setPredictionsOfUser(int matchNumber, User user, List<Prediction> predictionList) {
        String userID = user.getID();

        if (mPredictionOfUserMap.containsKey(userID)) {
            mPredictionOfUserMap.get(userID).add(matchNumber);
        } else {
            mPredictionOfUserMap.put(userID, new ArrayList<Integer>());
            mPredictionOfUserMap.get(userID).add(matchNumber);
        }

        Prediction prediction = null;
        for (Prediction p : predictionList) {
            if (p.getMatchNumber() == matchNumber && userID.equals(p.getUserID())) {
                prediction = p;
            }
        }
        if (prediction == null) {
            prediction = Prediction.emptyInstance(matchNumber, userID);
        }

        if (mMatchPredictionMap.containsKey(userID)) {
            mMatchPredictionMap.get(userID).put(matchNumber, prediction);
        } else {
            mMatchPredictionMap.put(userID, new SparseArray<Prediction>());
            mMatchPredictionMap.get(userID).put(matchNumber, prediction);
        }

    }

    public List<Prediction> getPredictionsOfUser(String userID) {
        if (mMatchPredictionMap.containsKey(userID)) {
            return toList(mMatchPredictionMap.get(userID));
        } else {
            return new ArrayList<>();
        }
    }

    private static <T> List<T> toList(SparseArray<T> predictionSparseArray) {
        if (predictionSparseArray == null)
            return new ArrayList<>();
        List<T> predictionList = new ArrayList<>(predictionSparseArray.size());
        for (int i = 0; i < predictionSparseArray.size(); i++)
            predictionList.add(predictionSparseArray.valueAt(i));
        return predictionList;
    }


    public List<Pair<User, Prediction>> getPredictionsOfUsers(int matchNumber, List<User> userList) {
        List<Pair<User, Prediction>> m = new ArrayList<>();

        for (User user : userList) {
            Prediction defaultPrediction = Prediction.emptyInstance(matchNumber, user.getID());
            Prediction p = !mMatchPredictionMap.containsKey(user.getID())?
                    defaultPrediction :
                    mMatchPredictionMap.get(user.getID()).get(matchNumber, defaultPrediction);

            m.add(new Pair<>(user, p));
        }
        return m;
    }

    public boolean wasPredictionFetched(User user, int matchNumber) {
        if (mPredictionOfUserMap.containsKey(user.getID())) {
            for (Integer matchNo : mPredictionOfUserMap.get(user.getID())) {
                if (matchNo == matchNumber)
                    return true;
            }
            return false;
        }
        return false;
    }

    public void addLeague(LeagueWrapper leagueWrapper) {

        mLeagueWrapperList.add(0, leagueWrapper);

        for (OnLeaguesChangedListener listener : mOnLeaguesChangedListenerSet) {
            listener.onLeaguesChanged();
        }

    }

    public void addUsersToLeague(String leagueID, List<LeagueUser> userList) {

        for (LeagueWrapper leagueWrapper : mLeagueWrapperList) {
            if (leagueWrapper.getLeague().getID().equals(leagueID)) {
                for (LeagueUser newUser : userList) {

                    boolean isUserOnList = false;
                    for (LeagueUser user : leagueWrapper.getLeagueUserList()) {
                        if (user.getUser().getID().equals(newUser.getUser().getID())) {
                            isUserOnList = true;
                            break;
                        }
                    }

                    if (!isUserOnList) {
                        leagueWrapper.getLeagueUserList().add(newUser);
                    }

                }

                Collections.sort(leagueWrapper.getLeagueUserList(), new Comparator<LeagueUser>() {
                    @Override
                    public int compare(LeagueUser o1, LeagueUser o2) {
                        return o1.getRank() - o2.getRank();
                    }
                });

                for (OnLeaguesChangedListener listener : mOnLeaguesChangedListenerSet) {
                    listener.onLeaguesChanged();
                }
            }
        }
    }

    public synchronized void removeLeague(LeagueWrapper leagueWrapper) {

        List<Integer> toRemoveIs = new ArrayList<>();
        for (int i = 0 ; i < mLeagueWrapperList.size() ; i++) {
            LeagueWrapper l = mLeagueWrapperList.get(i);
            if (l == null || l.getLeague() == null || l.getLeague().getID() == null) {
                toRemoveIs.add(i);
            }
            else if (leagueWrapper != null && leagueWrapper.getLeague() != null &&
                    l.getLeague().getID().equals(leagueWrapper.getLeague().getID())) {
                toRemoveIs.add(i);
            }
        }

        if (toRemoveIs.size() > 0) {
            for (Integer i : toRemoveIs) {
                mLeagueWrapperList.remove(i.intValue());
            }

            for (OnLeaguesChangedListener listener : mOnLeaguesChangedListenerSet) {
                listener.onLeaguesChanged();
            }
        }

    }

    public interface OnLatestPerformanceChangedListener {
        void onLatestPerformanceChanged();
    }

    public interface OnMatchesChangedListener {
        void onMatchesChanged();
    }

    public interface OnCountriesChangedListener {
        void onCountriesChanged();
    }

    public interface OnPredictionsChangedListener {
        void onPredictionsChanged();
    }

    public interface OnLeaguesChangedListener {
        void onLeaguesChanged();
    }

    public void addOnMatchesChangedListener(OnMatchesChangedListener listener) {
        mOnMatchesChangedListenerSet.add(listener);
    }

    public void removeOnMatchesChangedListener(OnMatchesChangedListener listener) {
        mOnMatchesChangedListenerSet.remove(listener);
    }

    public void addOnCountriesChangedListener(OnCountriesChangedListener listener) {
        mOnCountriesChangedListenerSet.add(listener);
    }

    public void removeOnCountriesChangedListener(OnCountriesChangedListener listener) {
        mOnCountriesChangedListenerSet.remove(listener);
    }

    public void addOnPredictionsChangedListener(OnPredictionsChangedListener listener) {
        mOnPredictionsChangedListenerSet.add(listener);
    }

    public void removeOnPredictionsChangedListener(OnPredictionsChangedListener listener) {
        mOnPredictionsChangedListenerSet.remove(listener);
    }

    public void addOnLatestPerformanceChangedListener(OnLatestPerformanceChangedListener listener) {
        mOnLatestPerformanceChangedListenerSet.add(listener);
    }

    public void removeOnLatestPerformanceChangedListener(OnLatestPerformanceChangedListener listener) {
        mOnLatestPerformanceChangedListenerSet.remove(listener);
    }

    public void addOnLeaguesChangedListener(OnLeaguesChangedListener listener) {
        mOnLeaguesChangedListenerSet.add(listener);
    }

    public void removeOnLeaguesChangedListener(OnLeaguesChangedListener listener) {
        mOnLeaguesChangedListenerSet.remove(listener);
    }
}
