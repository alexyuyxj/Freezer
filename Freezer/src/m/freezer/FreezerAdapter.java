package m.freezer;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class FreezerAdapter extends BaseAdapter implements Runnable, OnClickListener {
	private ListView view;
	private Context context;
	private HashMap<PackageInfo, Drawable> icons;
	private ArrayList<PackageInfo> recent;
	private ArrayList<PackageInfo> users;
	private ArrayList<PackageInfo> systems;
	private ArrayList<Object> items;
	private PackageManager pm;
	private Dialog pd;
	private Process p;

	public FreezerAdapter(ListView view) {
		this.view = view;
		this.context = view.getContext();
		icons = new HashMap<PackageInfo, Drawable>();
		items = new ArrayList<Object>();
		recent = new ArrayList<PackageInfo>();
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
			ArrayList<Entry<String, Integer>> recentPkgs = getRecentPackages();
			fillRecents(recentPkgs);
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
		List<PackageInfo> pis = pm.getInstalledPackages(0);
		Collections.sort(pis, new Comparator<PackageInfo>() {
			public int compare(PackageInfo lpi, PackageInfo rpi) {
				String left = String.valueOf(lpi.applicationInfo.loadLabel(pm));
				left = TextUtils.isEmpty(left) ? lpi.packageName : left;
				
				String right = String.valueOf(rpi.applicationInfo.loadLabel(pm));
				right = TextUtils.isEmpty(right) ? rpi.packageName : right;
				
				return left.compareTo(right);
			}
		});
		
		systems.clear();
		users.clear();
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
	
	private ArrayList<Entry<String, Integer>> getRecentPackages() {
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> map = (HashMap<String, Integer>) SPHelper.get(context, "recents");
		ArrayList<Entry<String, Integer>> recent = new ArrayList<Entry<String, Integer>>();
		if (map != null) {
			recent.addAll(map.entrySet());
		}
		
		ArrayList<SimpleEntry<PackageInfo, Entry<String, Integer>>> pis
				= new ArrayList<SimpleEntry<PackageInfo, Entry<String, Integer>>>();
		for (Entry<String, Integer> e : recent) {
			String pkg = e.getKey();
			PackageInfo tpi = null;
			for (PackageInfo pi : systems) {
				if (pi.packageName.equals(pkg)) {
					tpi = pi;
					break;
				}
			}
			if (tpi == null) {
				for (PackageInfo pi : users) {
					if (pi.packageName.equals(pkg)) {
						tpi = pi;
						break;
					}
				}
			}
			if (tpi != null) {
				pis.add(new SimpleEntry<PackageInfo, Entry<String, Integer>>(tpi, e));
			}
		}
		
		Collections.sort(pis, new Comparator<SimpleEntry<PackageInfo, Entry<String, Integer>>>() {
			public int compare(SimpleEntry<PackageInfo, Entry<String, Integer>> left,
					SimpleEntry<PackageInfo, Entry<String, Integer>> right) {
				Entry<String, Integer> le = left.getValue();
				Entry<String, Integer> re = right.getValue();
				int li = le.getValue();
				int ri = re.getValue();
				if (li == ri) {
					PackageInfo lpi = left.getKey();
					String ln = String.valueOf(lpi.applicationInfo.loadLabel(pm));
					ln = TextUtils.isEmpty(ln) ? lpi.packageName : ln;
					PackageInfo rpi = right.getKey();
					String rn = String.valueOf(rpi.applicationInfo.loadLabel(pm));
					rn = TextUtils.isEmpty(rn) ? rpi.packageName : rn;
					return ln.compareTo(rn);
				} else {
					return li < ri ? 1 : -1;
				}
			}
		});
		while (pis.size() > 5) {
			pis.remove(pis.size() - 1);
		}
		
		ArrayList<Entry<String, Integer>> recentPkgs = new ArrayList<Entry<String, Integer>>();
		for (SimpleEntry<PackageInfo, Entry<String, Integer>> e : pis) {
			recentPkgs.add(e.getValue());
		}
		return recentPkgs;
	} 
	
	private void fillRecents(ArrayList<Entry<String, Integer>> recentsPkgs) {
		recent.clear();
		for (Entry<String, Integer> e : recentsPkgs) {
			String pkg = e.getKey();
			boolean found = false;
			int i = 0;
			while (i < systems.size()) {
				PackageInfo pi = systems.get(i);
				if (pi.packageName.equals(pkg)) {
					systems.remove(i);
					recent.add(pi);
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
						recent.add(pi);
						break;
					}
					i++;
				}
			}
		}
	}
	
	private void fillItemData() {
		items.clear();
		if (recent.size() > 0) {
			items.add("Recent");
			items.addAll(recent);
		}
		if (users.size() > 0) {
			items.add("User");
			items.addAll(users);
		}
		if (systems.size() > 0) {
			items.add("System");
			items.addAll(systems);
		}
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
		ivIcon.setScaleType(ScaleType.FIT_CENTER);
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
					exec(pi.packageName, false);
				} else {
					exec(pi.packageName, true);
				}
			}
		}.start();
	}
	
	private void exec(String packageName, boolean toEnable) {
		try {
			refreshState(packageName, toEnable);
			if (p == null) {
				p = Runtime.getRuntime().exec("su");
			}
			OutputStream os = p.getOutputStream();
			String state = toEnable ? "enable" : "disable";
			String cmd = "pm " + state + " " + packageName + "\n";
			os.write(cmd.getBytes("ASCII"));
			os.flush();
		} catch (Throwable e) {
			destroy();
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
	
	private void refreshState(final String packageName, final boolean toEnable) {
		Handler handler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message msg) {
				try {
					PackageInfo pi = pm.getPackageInfo(packageName, 0);
					if ((toEnable && pi.applicationInfo.enabled)
							|| (!toEnable && !pi.applicationInfo.enabled)) {
						removeMessages(1);
						@SuppressWarnings("unchecked")
						HashMap<String, Integer> map = (HashMap<String, Integer>) SPHelper.get(context, "recents");
						if (map == null) {
							map = new HashMap<String, Integer>();
						}
						if (map.containsKey(packageName)) {
							map.put(packageName, map.get(packageName) + 1);
						} else {
							map.put(packageName, 1);
						}
						SPHelper.put(context, "recents", map);
						
						genList();
						if (pd != null && pd.isShowing()) {
							pd.dismiss();
						}
						return;
					}
				} catch (Throwable t) {}
				sendEmptyMessageDelayed(1, 100);
			}
		};
		handler.sendEmptyMessageDelayed(1, 100);
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
	
	public void destroy() {
		if (p != null) {
			Process pp = p;
			p = null;
			pp.destroy();
		}
	}
	
}
