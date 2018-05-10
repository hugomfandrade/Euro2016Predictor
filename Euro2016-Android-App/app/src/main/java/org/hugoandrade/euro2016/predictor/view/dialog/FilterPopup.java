package org.hugoandrade.euro2016.predictor.view.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.hugoandrade.euro2016.predictor.R;

import java.util.List;

public class FilterPopup extends PopupWindow {

    private final Context mContext;
    private final View mParentView;
    private final List<String> mFilterList;
    private final int mMaxRows;

    private OnFilterItemClickedListener mListener;

    public FilterPopup(View view, List<String> filterList, int startingPosition) {
        this(view, filterList, startingPosition, 0);
    }

    public FilterPopup(View view, List<String> filterList, int startingPosition, int maxRows) {
        super(View.inflate(view.getContext(), R.layout.layout_popup, null),
                view.getWidth(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        mContext = view.getContext();
        mParentView = view;
        mFilterList = filterList;
        mMaxRows = maxRows;

        initializeUI(startingPosition);
    }

    private void initializeUI(int startingPosition) {

        RecyclerView rvFilter = getContentView().findViewById(R.id.rv_filter);
        rvFilter.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        rvFilter.setAdapter(new FilterListAdapter());

        if (mMaxRows > 0) {
            ViewGroup.LayoutParams params = rvFilter.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    mMaxRows * 40,
                    mContext.getResources().getDisplayMetrics());
            rvFilter.setLayoutParams(params);
        }
        rvFilter.scrollToPosition(startingPosition);

        showAsDropDown(mParentView, 0,0);
    }

    public void setOnFilterItemClickedListener(OnFilterItemClickedListener listener) {
        mListener = listener;
    }

    public interface OnFilterItemClickedListener {
        void onFilterItemClicked(int position);
    }

    private class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.ViewHolder> {

        FilterListAdapter() { }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater vi = LayoutInflater.from(parent.getContext());
            return new ViewHolder(vi.inflate(R.layout.list_item_filter, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            String filter = mFilterList.get(holder.getAdapterPosition());

            holder.tvFilter.setText(filter);
        }

        @Override
        public int getItemCount() {
            return mFilterList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvFilter;

            ViewHolder(View itemView) {
                super(itemView);

                tvFilter = itemView.findViewById(R.id.tv_filter);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null)
                            mListener.onFilterItemClicked(getAdapterPosition());
                        dismiss();
                    }
                });
            }
        }
    }
}
