package de.wiosense.wiokey.ui;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.wiosense.wiokey.R;

public class ExpandAdapter extends RecyclerView.Adapter<ExpandAdapter.MyViewHolder> {

    private List<ExpandItem> mExpandItemList;
    public int viewResource;

    public static class ExpandItem {
        public int question;
        public int answer;
        public boolean isExpanded;
        public String extraString;

        public ExpandItem(int question, int answer, String extraString){
            this.isExpanded = false;
            this.question = question;
            this.answer = answer;
            this.extraString = extraString;
        }

        public ExpandItem(int question, int answer){
            this(question, answer, null);
        }
    }

    public ExpandAdapter(List<ExpandItem> questions, int viewResource){
        this.mExpandItemList = questions;
        this.viewResource = viewResource;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(viewResource,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.mTextQuestion.setText(mExpandItemList.get(position).question);
        holder.mTextAnswer.setText(mExpandItemList.get(position).answer);
        holder.mTextAnswer.setMovementMethod(LinkMovementMethod.getInstance());
        String extraString = mExpandItemList.get(position).extraString;
        if (extraString != null) {
            holder.mTextAnswer.append(extraString);
        }

        boolean isExpanded = mExpandItemList.get(position).isExpanded;
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mExpandItemList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView mTextQuestion, mTextAnswer;
        ConstraintLayout expandableLayout;

        MyViewHolder(@NonNull View itemView){
            super(itemView);

            mTextQuestion = itemView.findViewById(R.id.text_question);
            mTextAnswer = itemView.findViewById(R.id.text_answer);
            expandableLayout = itemView.findViewById(R.id.layout_expand);

            mTextQuestion.setOnClickListener(this);
            mTextAnswer.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            ExpandItem mExpandItem = mExpandItemList.get(getAdapterPosition());
            mExpandItem.isExpanded = !mExpandItem.isExpanded;
            notifyItemChanged(getAdapterPosition());
        }
    }
}
