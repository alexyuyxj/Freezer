package cn.sharerec.gui.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SrecProgressDialog extends RelativeLayout {
	public static final int ID_PB = 1;
	public static final int ID_TV_PROGRESS = 2;
	public ProgressBar pb;
	public TextView tvProgress;

	public SrecProgressDialog(Context context) {
		super(context);
		init(context);
	}

	public SrecProgressDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SrecProgressDialog(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		pb = new ProgressBar(context);
		pb.setId(ID_PB);
		int dp_10 = (int) (getResources().getDisplayMetrics().density * 10f + 0.5f);
		pb.setPadding(dp_10, dp_10, dp_10, dp_10);
		addView(pb);
		
		tvProgress = new TextView(context);
		tvProgress.setId(ID_TV_PROGRESS);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(ALIGN_LEFT, pb.getId());
		lp.addRule(ALIGN_TOP, pb.getId());
		lp.addRule(ALIGN_RIGHT, pb.getId());
		lp.addRule(ALIGN_BOTTOM, pb.getId());
		tvProgress.setGravity(Gravity.CENTER);
		tvProgress.setPadding(dp_10, dp_10, dp_10, dp_10);
		tvProgress.setTextColor(0xffffffff);
		tvProgress.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp_10);
		addView(tvProgress, lp);
	}
	
}
