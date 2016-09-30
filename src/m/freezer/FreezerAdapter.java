package m.freezer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FreezerAdapter extends BaseAdapter implements Runnable, OnClickListener {
	private ListView view;
	private Context context;
	private HashMap<PackageInfo, Drawable> icons;
	private ArrayList<PackageInfo> recents;
	private ArrayList<PackageInfo> users;
	private ArrayList<PackageInfo> systems;
	private ArrayList<Object> items;
	private PackageManager pm;
	private Dialog pd;

	public FreezerAdapter(ListView view) {
		this.view = view;
		this.context = view.getContext();
		icons = new HashMap<PackageInfo, Drawable>();
		items = new ArrayList<Object>();
		recents = new ArrayList<PackageInfo>();
		users = new ArrayList<PackageInfo>();
		systems = new ArrayList<PackageInfo>();
		pm = context.getPackageManager();
	}
	
	private int dp2px(int dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return (int) (dp * density + 0.5f);
	}
	
	public void genList() {
		new Thread(this).start();
	}
	
	public void run() {
		try {
			fillLists();
			ArrayList<String> recentsPkgs = getRecentPackages();
			fillRecents(recentsPkgs);
			fillItemData();
			view.post(new Runnable() {
				public void run() {
					notifyDataSetChanged();
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private boolean isSystemApp(PackageInfo pi) {
		boolean isSysApp = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
		boolean isSysUpd = (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1;
		return isSysApp || isSysUpd;
	}
	
	private void fillLists() throws Throwable {
		systems.clear();
		users.clear();
		List<PackageInfo> pis = pm.getInstalledPackages(0);
		String myPkg = context.getPackageName();
		for (PackageInfo pi : pis) {
			if (!pi.applicationInfo.packageName.equals(myPkg)) {
				if (isSystemApp(pi)) {
					systems.add(pi);
				} else {
					users.add(pi);
				}
			}
		}
	}
	
	private ArrayList<String> getRecentPackages() {
		@SuppressWarnings("unchecked")
		ArrayList<String> pkgs = (ArrayList<String>) SPHelper.get(context, "recents");
		ArrayList<String> recentsPkgs = new ArrayList<String>();
		if (pkgs != null) {
			for (String pkg : pkgs) {
				boolean found = false;
				for (PackageInfo pi : systems) {
					if (pi.packageName.equals(pkg)) {
						found = true;
						break;
					}
				}
				if (!found) {
					for (PackageInfo pi : users) {
						if (pi.packageName.equals(pkg)) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					recentsPkgs.add(pkg);
				}
			}
			SPHelper.put(context, "recents", recentsPkgs);
		}
		return recentsPkgs;
	} 
	
	private void fillRecents(ArrayList<String> recentsPkgs) {
		recents.clear();
		for (String pkg : recentsPkgs) {
			boolean found = false;
			int i = 0;
			while (i < systems.size()) {
				PackageInfo pi = systems.get(i);
				if (pi.packageName.equals(pkg)) {
					systems.remove(i);
					recents.add(pi);
					found = true;
					break;
				}
				i++;
			}
			if (!found) {
				i = 0;
				while (i < users.size()) {
					PackageInfo pi = users.get(i);
					if (pi.packageName.equals(pkg)) {
						users.remove(i);
						recents.add(pi);
						break;
					}
					i++;
				}
			}
		}
	}
	
	private void fillItemData() {
		items.clear();
		if (recents.size() > 0) {
			items.add("Recent");
		}
		items.addAll(recents);
		if (users.size() > 0) {
			items.add("User");
		}
		items.addAll(users);
		if (systems.size() > 0) {
			items.add("System");
		}
		items.addAll(systems);
	}
	
	public int getCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public int getViewTypeCount() {
		return 2;
	}
	
	public int getItemViewType(int position) {
		if (getItem(position) instanceof String) {
			return 0;
		} else {
			return 1;
		}
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == 0) {
			return getView0(position, convertView, parent);
		} else {
			return getView1(position, convertView, parent);
		}
	}
	
	private View getView0(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = genTitleView();
		}
		
		TextView tvTitle = (TextView) convertView;
		String title = (String) getItem(position);
		tvTitle.setText(title);
		return convertView;
	}

	private View getView1(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = genItemView();
		}
		
		LinearLayout llItem = (LinearLayout) convertView;
		ImageView ivIcon = (ImageView) llItem.getChildAt(0);
		LinearLayout llInfo = (LinearLayout) llItem.getChildAt(1);
		TextView tvName = (TextView) llInfo.getChildAt(0);
		TextView tvPackage = (TextView) llInfo.getChildAt(1);
		FrameLayout flFreeze = (FrameLayout) llItem.getChildAt(2);
		TextView tvFreeze = (TextView) flFreeze.getChildAt(0);
		
		PackageInfo pi = (PackageInfo) getItem(position);
		ivIcon.setImageDrawable(getIcon(pi));
		String name = String.valueOf(pi.applicationInfo.loadLabel(pm));
		tvName.setText(TextUtils.isEmpty(name) ? pi.packageName : name);
		tvPackage.setText(pi.packageName);
		if (pi.applicationInfo.enabled) {
			tvFreeze.setBackgroundColor(0);
			tvFreeze.setText(R.string.freeze);
			tvFreeze.setTextColor(0xffffffff);
		} else {
			tvFreeze.setBackgroundColor(0xffffffff);
			tvFreeze.setText(R.string.unfreeze);
			tvFreeze.setTextColor(0xff33a6e8);
		}
		tvFreeze.setTag(pi);
		
		return convertView;
	}
	
	private TextView genTitleView() {
		TextView tvTitle = new TextView(context);
		tvTitle.setBackgroundColor(0xffefefef);
		tvTitle.setSingleLine();
		tvTitle.setTextColor(0xff353535);
		int dp_3 = dp2px(3);
		int dp_10 = dp2px(10);
		tvTitle.setPadding(dp_10, dp_3, dp_10, dp_3);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
		return tvTitle;
	}

	private View genItemView() {
		LinearLayout llItem = new LinearLayout(context);
		int dp_5 = dp2px(5);
		llItem.setPadding(0, dp_5, 0, dp_5);
		
		ImageView ivIcon = new ImageView(context);
		ivIcon.setScaleType(ScaleType.CENTER_INSIDE);
		int dp_48 = dp2px(48);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp_48, dp_48);
		lp.gravity = Gravity.CENTER_VERTICAL;
		int dp_10 = dp2px(10);
		lp.leftMargin = dp_10;
		llItem.addView(ivIcon, lp);
		
		LinearLayout llInfo = new LinearLayout(context);
		llInfo.setOrientation(LinearLayout.VERTICAL);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_VERTICAL;
		lp.leftMargin = lp.rightMargin = dp_10;
		lp.weight = 1;
		llItem.addView(llInfo, lp);
		
		TextView tvName = new TextView(context);
		tvName.setSingleLine();
		tvName.setTextColor(0xff353535);
		tvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llInfo.addView(tvName, lp);

		TextView tvPackage = new TextView(context);
		tvPackage.setSingleLine();
		tvPackage.setTextColor(0xffafafaf);
		tvPackage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
		lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		llInfo.addView(tvPackage, lp);
		
		FrameLayout flFreeze = new FrameLayout(context);
		flFreeze.setBackgroundColor(0xff33a6e8);
		lp = new LinearLayout.LayoutParams(dp2px(72), dp2px(30));
		lp.rightMargin = dp_10;
		lp.gravity = Gravity.CENTER_VERTICAL;
		llItem.addView(flFreeze, lp);
		
		TextView tvFreeze = new TextView(context);
		tvFreeze.setGravity(Gravity.CENTER);
		tvFreeze.setTextColor(0xffffffff);
		tvFreeze.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		tvFreeze.setText(R.string.freeze);
		tvFreeze.setOnClickListener(this);
		tvFreeze.setBackgroundColor(0);
		FrameLayout.LayoutParams lpfl = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		int dp_1 = dp2px(1);
		lpfl.setMargins(dp_1, dp_1, dp_1, dp_1);
		flFreeze.addView(tvFreeze, lpfl);
		
		return llItem;
	}

	private Drawable getIcon(PackageInfo pi) {
		Drawable icon = icons.get(pi);
		if (icon == null) {
			icon = pi.applicationInfo.loadIcon(pm);
			icons.put(pi, icon);
		}
		return icon;
	}
	
	public void onClick(final View v) {
		if (pd != null && pd.isShowing()) {
			pd.dismiss();
		}
		pd = getProgressDialog();
		pd.show();
		
		new Thread() {
			public void run() {
				PackageInfo pi = (PackageInfo) v.getTag();
				if (pi.applicationInfo.enabled) {
					exec(pi.packageName, "disable");
				} else {
					exec(pi.packageName, "enable");
				}
			}
		}.start();
	}
	
	private void exec(final String packageName, String state) {
		try {
			final Process p = Runtime.getRuntime().exec("su");
			OutputStream os = p.getOutputStream();
			String cmd = "pm " + state + " " + packageName + "\nexit\n";
			os.write(cmd.getBytes("ASCII"));
			os.flush();
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				System.out.println(line);
				line = br.readLine();
			}
			
			view.postDelayed(new Runnable() {
				public void run() {
					p.destroy();
					
					@SuppressWarnings("unchecked")
					ArrayList<String> pkgs = (ArrayList<String>) SPHelper.get(context, "recents");
					if (pkgs == null) {
						pkgs = new ArrayList<String>();
					}
					if (pkgs.contains(packageName)) {
						pkgs.remove(packageName);
					}
					pkgs.add(0, packageName);
					while (pkgs.size() > 5) {
						pkgs.remove(pkgs.size() - 1);
					}
					SPHelper.put(context, "recents", pkgs);
					
					genList();
					if (pd != null && pd.isShowing()) {
						pd.dismiss();
					}
				}
			}, 5000);
		} catch (Throwable e) {
			view.post(new Runnable() {
				public void run() {
					if (pd != null && pd.isShowing()) {
						pd.dismiss();
					}
				}
			});
			throw new RuntimeException(e);
		}
	}
	
	private Dialog getProgressDialog() {
		ProgressBar pb = new ProgressBar(context);
		int dp_10 = (int) (context.getResources().getDisplayMetrics().density * 10f + 0.5f);
		pb.setPadding(dp_10, dp_10, dp_10, dp_10);
		
		Dialog pd = new Dialog(context, R.style.common_dialog);
		pd.setCancelable(false);
		pd.setContentView(pb);
		return pd;
	}
	
}
