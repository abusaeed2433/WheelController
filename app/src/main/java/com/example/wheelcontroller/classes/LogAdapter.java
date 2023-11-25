package com.example.wheelcontroller.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wheelcontroller.R;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private final List<EachLog> allLogs;
    private final Context mContext;
    private EachLog curItem;

    public LogAdapter(Context mContext, List<EachLog> allLogs) {
        this.mContext = mContext;
        this.allLogs = allLogs;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.single_log_layout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        curItem = allLogs.get(position);
        holder.tvLogType.setText(curItem.getType());
        holder.tvLogMessage.setText(curItem.getMessage());
        holder.tvTimestamp.setText(curItem.getTimestamp());

        holder.tvLogType.setTextColor( curItem.getTypeColor() );
        holder.tvLogMessage.setTextColor( curItem.getMessageColor() );

    }

    @Override
    public int getItemCount() {
        return allLogs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private final TextView tvLogType;
        private final TextView tvLogMessage;
        private final TextView tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogType = itemView.findViewById(R.id.tvLogType);
            tvLogMessage = itemView.findViewById(R.id.tvLogMessage);
            tvTimestamp = itemView.findViewById(R.id.tvLogTimestamp);
        }
    }

}
