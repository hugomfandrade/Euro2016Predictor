package org.hugoandrade.euro2016.predictor.view.listadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hugoandrade.euro2016.predictor.GlobalData;
import org.hugoandrade.euro2016.predictor.R;
import org.hugoandrade.euro2016.predictor.data.LeagueWrapper;
import org.hugoandrade.euro2016.predictor.data.raw.User;

import java.util.ArrayList;
import java.util.List;

public class LeagueListAdapter extends RecyclerView.Adapter<LeagueListAdapter.ViewHolder> {

    private List<LeagueWrapper> mLeagueList;

    private OnItemClickListener mListener;

    public LeagueListAdapter() {
        mLeagueList = new ArrayList<>();
    }

    @NonNull
    @Override
    public LeagueListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater vi = LayoutInflater.from(parent.getContext());
        return new ViewHolder(vi.inflate(R.layout.list_item_league, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final LeagueListAdapter.ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        LeagueWrapper leagueWrapper = mLeagueList.get(position);

        holder.leagueStandingListAdapter.set(leagueWrapper.getUserList());
        holder.rvLeagueStandings.setAdapter(holder.leagueStandingListAdapter);

        holder.tvLeagueName.setText(leagueWrapper.getLeague().getName());
        holder.tvLeagueMembers.setText(TextUtils.concat("(",
                String.valueOf(leagueWrapper.getLeague().getNumberOfMembers()),
                " ",
                context.getString(R.string.members),
                ")"));

    }

    public void set(List<LeagueWrapper> leagueWrapperList) {
        mLeagueList = leagueWrapperList;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mLeagueList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(LeagueWrapper leagueWrapper);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView tvLeagueName;
        TextView tvLeagueMembers;
        TextView tvLeagueDetails;
        RecyclerView rvLeagueStandings;

        LeagueStandingListAdapter leagueStandingListAdapter = new LeagueStandingListAdapter();


        ViewHolder(View itemView) {
            super(itemView);

            tvLeagueName = itemView.findViewById(R.id.tv_league_name);
            tvLeagueMembers = itemView.findViewById(R.id.tv_league_members);
            rvLeagueStandings = itemView.findViewById(R.id.rv_league_standings);
            rvLeagueStandings.setNestedScrollingEnabled(false);
            rvLeagueStandings.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false));
            rvLeagueStandings.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            tvLeagueDetails = itemView.findViewById(R.id.tv_league_details);
            tvLeagueDetails.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null)
                mListener.onItemClick(mLeagueList.get(getAdapterPosition()));
        }
    }
}