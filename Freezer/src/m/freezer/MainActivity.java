package m.freezer;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class MainActivity extends Activity {
	private FreezerAdapter adapter;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
		
		ListView lv = new ListView(this);
		lv.setBackgroundColor(0xffffffff);
		lv.setCacheColorHint(0);
		lv.setDivider(new ColorDrawable(0xffe8e8e8));
		lv.setDividerHeight((int) (getResources().getDisplayMetrics().density + 0.5f));
		setContentView(lv);
		
		adapter = new FreezerAdapter(lv);
		lv.setAdapter(adapter);
		adapter.genList();
	}
	
	public void finish() {
		if (adapter != null) {
			adapter.destroy();
		}
		super.finish();
		System.exit(0);
	}
	
}
