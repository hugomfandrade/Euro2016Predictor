package org.hugoandrade.euro2016.predictor.model.service;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import org.hugoandrade.euro2016.predictor.data.Country;
import org.hugoandrade.euro2016.predictor.data.LoginData;
import org.hugoandrade.euro2016.predictor.data.Match;
import org.hugoandrade.euro2016.predictor.data.Prediction;
import org.hugoandrade.euro2016.predictor.data.User;
import org.hugoandrade.euro2016.predictor.model.IMobileClientService;
import org.hugoandrade.euro2016.predictor.model.IMobileClientServiceCallback;
import org.hugoandrade.euro2016.predictor.model.parser.MobileClientData;
import org.hugoandrade.euro2016.predictor.network.MobileServiceAdapter;
import org.hugoandrade.euro2016.predictor.network.MobileServiceCallback;
import org.hugoandrade.euro2016.predictor.network.MobileServiceData;
import org.hugoandrade.euro2016.predictor.network.MultipleCloudStatus;

import java.util.ArrayList;
import java.util.Collections;

public class MobileService extends LifecycleLoggingService {

    private IMobileClientServiceCallback mCallback;


    private final Object syncObj = new Object();

    public static Intent makeIntent(Context context) {
        return new Intent(context, MobileService.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            MobileServiceAdapter.Initialize(getApplicationContext());
        }
        catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MobileServiceAdapter.unInitialize(getApplicationContext());
    }

    /**
     *  Local-side IPC implementation stub class and constructs the stub
     *  and attaches it to the interface.
     *  */
    private final IMobileClientService.Stub mBinder = new IMobileClientService.Stub() {

        @Override
        public void registerCallback(IMobileClientServiceCallback cb) throws RemoteException {
            mCallback = cb;
        }

        @Override
        public void unregisterCallback(IMobileClientServiceCallback cb) throws RemoteException {
            if (mCallback == cb)
                mCallback = null;
        }

        @Override
        public boolean getSystemData() {

            MobileServiceCallback i = MobileServiceAdapter.getInstance().getSystemData();
            MobileServiceCallback.addCallback(i, new MobileServiceCallback.OnResult() {

                @Override
                public void onResult(MobileServiceData data) {

                    int requestResult = data.wasSuccessful() ?
                            MobileClientData.REQUEST_RESULT_SUCCESS:
                            MobileClientData.REQUEST_RESULT_FAILURE;

                    MobileClientData m = MobileClientData.makeMessage(
                            MobileClientData.OperationType.GET_SYSTEM_DATA.ordinal(),
                            requestResult);
                    m.setSystemData(data.getSystemData());
                    m.setErrorMessage(data.getMessage());

                    sendMobileDataMessage(m);
                }
            });
            return true;
        }

        @Override
        public boolean login(final LoginData loginData) {

            MobileServiceCallback i = MobileServiceAdapter.getInstance().login(loginData);
            MobileServiceCallback.addCallback(i, new MobileServiceCallback.OnResult() {

                @Override
                public void onResult(MobileServiceData data) {

                    int requestResult = data.wasSuccessful() ?
                            MobileClientData.REQUEST_RESULT_SUCCESS:
                            MobileClientData.REQUEST_RESULT_FAILURE;

                    MobileClientData m = MobileClientData.makeMessage(
                            MobileClientData.OperationType.LOGIN.ordinal(),
                            requestResult);

                    if (data.wasSuccessful()) {
                        LoginData resultLoginData = data.getLoginData();
                        resultLoginData.setPassword(loginData.getPassword());

                        MobileServiceUser mobileServiceUser = new MobileServiceUser(resultLoginData.getUserID());
                        mobileServiceUser.setAuthenticationToken(resultLoginData.getToken());
                        MobileServiceAdapter.getInstance().setMobileServiceUser(mobileServiceUser);

                        m.setLoginData(resultLoginData);
                    }
                    else {
                        m.setErrorMessage(data.getMessage());
                    }

                    sendMobileDataMessage(m);
                }
            });
            return true;
        }

        @Override
        public boolean signUp(final LoginData loginData) {

            MobileServiceCallback i = MobileServiceAdapter.getInstance().signUp(loginData);
            MobileServiceCallback.addCallback(i, new MobileServiceCallback.OnResult() {

                @Override
                public void onResult(MobileServiceData data) {

                    int requestResult = data.wasSuccessful() ?
                            MobileClientData.REQUEST_RESULT_SUCCESS:
                            MobileClientData.REQUEST_RESULT_FAILURE;

                    MobileClientData m = MobileClientData.makeMessage(
                            MobileClientData.OperationType.REGISTER.ordinal(),
                            requestResult);

                    if (data.wasSuccessful()) {
                        m.setLoginData(data.getLoginData());
                    }
                    else {
                        m.setErrorMessage(data.getMessage());
                    }

                    sendMobileDataMessage(m);
                }
            });
            return true;
        }

        @Override
        public boolean getInfo(String userID) {

            final MobileClientData m = MobileClientData.makeMessage(
                    MobileClientData.OperationType.GET_INFO.ordinal(),
                    MobileClientData.REQUEST_RESULT_SUCCESS);

            final MultipleCloudStatus n = new MultipleCloudStatus(4);

            MobileServiceCallback iCountries = MobileServiceAdapter.getInstance().getCountries();
            MobileServiceCallback.addCallback(iCountries, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {
                    synchronized (syncObj) {

                        if (n.isAborted()) return; // An error occurred
                        n.operationCompleted();

                        if (data.wasSuccessful()) {
                            ArrayList<Country> countryList = new ArrayList<>(data.getCountryList());

                            Collections.sort(countryList);

                            m.setCountryList(countryList);

                            if (n.isFinished())
                                sendMobileDataMessage(m);

                        } else {
                            n.abort();
                            Log.e(TAG, "sendErrorMessage: (" + 1 + ") " + data.getMessage());
                            sendErrorMessage(MobileClientData.OperationType.GET_INFO.ordinal(), data.getMessage());
                        }
                    }
                }
            });

            MobileServiceCallback iMatches = MobileServiceAdapter.getInstance().getMatches();
            MobileServiceCallback.addCallback(iMatches, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {
                    synchronized (syncObj) {

                        if (n.isAborted()) return; // An error occurred
                        n.operationCompleted();

                        if (data.wasSuccessful()) {
                            ArrayList<Match> matchList = new ArrayList<>(data.getMatchList());

                            Collections.sort(matchList);

                            m.setMatchList(matchList);

                            if (n.isFinished())
                                sendMobileDataMessage(m);

                        } else {
                            n.abort();
                            Log.e(TAG, "sendErrorMessage: (" + 2 + ") " + data.getMessage());
                            sendErrorMessage(MobileClientData.OperationType.GET_INFO.ordinal(), data.getMessage());
                        }
                    }
                }
            });

            MobileServiceCallback iUsers = MobileServiceAdapter.getInstance().getUsers();
            MobileServiceCallback.addCallback(iUsers, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {
                    synchronized (syncObj) {

                        if (n.isAborted()) return; // An error occurred
                        n.operationCompleted();

                        if (data.wasSuccessful()) {

                            m.setUsers(data.getUserList());

                            if (n.isFinished())
                                sendMobileDataMessage(m);

                        } else {
                            n.abort();
                            Log.e(TAG, "sendErrorMessage: (" + 3 + ") " + data.getMessage());
                            sendErrorMessage(MobileClientData.OperationType.GET_INFO.ordinal(), data.getMessage());
                        }
                    }
                }
            });

            MobileServiceCallback iPredictions = MobileServiceAdapter.getInstance().getPredictions(userID);
            MobileServiceCallback.addCallback(iPredictions, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {
                    synchronized (syncObj) {

                        if (n.isAborted()) return; // An error occurred
                        n.operationCompleted();

                        if (data.wasSuccessful()) {

                            m.setPredictionList(data.getPredictionList());

                            if (n.isFinished())
                                sendMobileDataMessage(m);

                        } else {
                            n.abort();
                            Log.e(TAG, "sendErrorMessage: (" + 4 + ") " + data.getMessage());
                            sendErrorMessage(MobileClientData.OperationType.GET_INFO.ordinal(), data.getMessage());
                        }
                    }
                }
            });
            return true;
        }

        @Override
        public boolean putPrediction(final Prediction prediction) {

            MobileServiceCallback i = MobileServiceAdapter.getInstance().insertPrediction(prediction);
            MobileServiceCallback.addCallback(i, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {

                    int requestResult = data.wasSuccessful() ?
                            MobileClientData.REQUEST_RESULT_SUCCESS:
                            MobileClientData.REQUEST_RESULT_FAILURE;

                    MobileClientData m = MobileClientData.makeMessage(
                            MobileClientData.OperationType.PUT_PREDICTION.ordinal(),
                            requestResult);

                    m.setPrediction(data.getPrediction());
                    m.setErrorMessage(data.getMessage());

                    sendMobileDataMessage(m);
                }
            });
            return true;
        }

        @Override
        public boolean getPredictions(final User user) {
            MobileServiceCallback i = MobileServiceAdapter.getInstance().getPredictions(user.getID());
            MobileServiceCallback.addCallback(i, new MobileServiceCallback.OnResult() {
                @Override
                public void onResult(MobileServiceData data) {

                    int requestResult = data.wasSuccessful() ?
                        MobileClientData.REQUEST_RESULT_SUCCESS:
                        MobileClientData.REQUEST_RESULT_FAILURE;

                    MobileClientData m = MobileClientData.makeMessage(
                            MobileClientData.OperationType.GET_PREDICTIONS.ordinal(),
                            requestResult);
                    m.setUser(user);
                    m.setPredictionList(data.getPredictionList());
                    m.setErrorMessage(data.getMessage());

                    sendMobileDataMessage(m);
                }
            });
            return true;
        }
    };

    /**
     * Sends a callback error message with a failure operation result flag
     * and with the given operation type flag
     */
    private void sendErrorMessage(int operationType, String message) {
        MobileClientData m = MobileClientData.makeMessage(
                operationType,
                MobileClientData.REQUEST_RESULT_FAILURE);
        m.setErrorMessage(message);

        sendMobileDataMessage(m);
    }

    public void sendMobileDataMessage(MobileClientData mobileClientData) {
        try {
            if (mCallback != null)
                mCallback.sendResults(mobileClientData);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}