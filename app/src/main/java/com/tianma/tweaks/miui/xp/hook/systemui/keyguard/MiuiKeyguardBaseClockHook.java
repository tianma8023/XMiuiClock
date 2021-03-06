package com.tianma.tweaks.miui.xp.hook.systemui.keyguard;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tianma.tweaks.miui.R;
import com.tianma.tweaks.miui.cons.PrefConst;
import com.tianma.tweaks.miui.data.http.repository.DataRepository;
import com.tianma.tweaks.miui.utils.ResolutionUtils;
import com.tianma.tweaks.miui.utils.SPUtils;
import com.tianma.tweaks.miui.utils.XLog;
import com.tianma.tweaks.miui.utils.XSPUtils;
import com.tianma.tweaks.miui.xp.hook.BaseSubHook;
import com.tianma.tweaks.miui.xp.hook.systemui.hitokoto.OneSentenceManager;
import com.tianma.tweaks.miui.xp.hook.systemui.screen.ScreenBroadcastManager;
import com.tianma.tweaks.miui.xp.hook.systemui.screen.SimpleScreenListener;
import com.tianma.tweaks.miui.xp.wrapper.MethodHookWrapper;
import com.tianma.tweaks.miui.xp.wrapper.XposedWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.tianma.tweaks.miui.xp.wrapper.XposedWrapper.hookAllConstructors;

/**
 * MIUI 锁屏界面 Hook
 * com.android.keyguard.MiuiKeyguardBaseClock
 */
public class MiuiKeyguardBaseClockHook extends BaseSubHook {

    private static final String CLASS_MIUI_KEYGUARD_BASE_CLOCK = "com.android.keyguard.MiuiKeyguardBaseClock";

    private Class<?> mKeyguardBaseClockClass;

    private List<View> mKeyguardClockList = new ArrayList<>();

    private CompositeDisposable mCompositeDisposable;

    private Context modContext;

    private boolean oneSentenceEnabled;
    private int oneSentenceColor;
    private float oneSentenceTextSize;

    public MiuiKeyguardBaseClockHook(ClassLoader classLoader, XSharedPreferences xsp) {
        super(classLoader, xsp);
        mCompositeDisposable = new CompositeDisposable();
        oneSentenceEnabled = XSPUtils.oneSentenceEnabled(xsp);
        oneSentenceColor = XSPUtils.getOneSentenceColor(xsp);
        oneSentenceTextSize = XSPUtils.getOneSentenceTextSize(xsp);
    }

    @Override
    public void startHook() {
        try {
            mKeyguardBaseClockClass = XposedWrapper.findClass(CLASS_MIUI_KEYGUARD_BASE_CLOCK, mClassLoader);
            if (oneSentenceEnabled) {
                XLog.d("OneSentence enabled, hooking MiuiKeyguardBaseClock...");

                hookOnFinishInflate();
                hookHandleNotificationChange();
                hookConstructor();
            }
        } catch (Throwable t) {
            XLog.e("Error occurs when hook MiuiKeyguardBaseClock", t);
        }
    }

    private void hookOnFinishInflate() {
        XposedWrapper.findAndHookMethod(mKeyguardBaseClockClass,
                "onFinishInflate",
                new MethodHookWrapper() {
                    @Override
                    protected void after(MethodHookParam param) {
                        LinearLayout miuiKeyguardBaseClock = (LinearLayout) param.thisObject;
                        Context context = miuiKeyguardBaseClock.getContext();
//                        miuiKeyguardBaseClock.setBackgroundColor(Color.parseColor("#892719"));

//                        TextView mLunarCalendarInfo = (TextView) XposedHelpers.getObjectField(miuiKeyguardBaseClock, "mLunarCalendarInfo");
//                        mLunarCalendarInfo.setBackgroundColor(Color.parseColor("#791230"));

                        TextView mOwnerInfo = (TextView) XposedHelpers.getObjectField(miuiKeyguardBaseClock, "mOwnerInfo");
//                        LinearLayout.LayoutParams mOwnerInfoLayoutParams = (LinearLayout.LayoutParams) mOwnerInfo.getLayoutParams();
//                        mOwnerInfo.setBackgroundColor(Color.parseColor("#ff4081"));
//                        mOwnerInfoLayoutParams.gravity = Gravity.CENTER_HORIZONTAL;

                        int mOwnerInfoIndex = miuiKeyguardBaseClock.indexOfChild(mOwnerInfo);
//                        XLog.d("mOwnerInfoIndex = " + mOwnerInfoIndex);

                        TextView hitokotoTextView = new TextView(context);
                        hitokotoTextView.setTextSize(oneSentenceTextSize);
                        hitokotoTextView.setTextColor(oneSentenceColor);
//                        hitokotoTextView.setBackgroundColor(Color.parseColor("#402384"));
                        hitokotoTextView.setId(R.id.hitokoto_info_text_view);
//                        hitokotoTextView.setText("Hitokoto");
//                        hitokotoTextView.setVisibility(View.GONE);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        hitokotoTextView.setLayoutParams(params);
                        params.gravity = Gravity.CENTER;
                        params.topMargin = (int) ResolutionUtils.dp2px(miuiKeyguardBaseClock.getContext(), 8.95f);

                        miuiKeyguardBaseClock.addView(hitokotoTextView, mOwnerInfoIndex + 1);
                    }
                });
    }

    private void hookHandleNotificationChange() {
        XposedWrapper.findAndHookMethod(mKeyguardBaseClockClass,
                "handleNotificationChange",
                new MethodHookWrapper() {
                    @Override
                    protected void after(MethodHookParam param) {
                        View keyguardClock = (View) param.thisObject;

                        boolean mHasNotification = XposedHelpers.getBooleanField(keyguardClock, "mHasNotification");

                        try {
                            TextView hitokotoInfo = keyguardClock.findViewById(R.id.hitokoto_info_text_view);
                            if (hitokotoInfo != null) {
                                hitokotoInfo.setVisibility(mHasNotification ? View.GONE : View.VISIBLE);
                            }
                        } catch (Throwable t) {
                            // ignore
                            XLog.e("", t);
                        }
                    }
                });
    }

    // com.android.keyguard.MiuiKeyguardBaseClock#constructor()
    private void hookConstructor() {
        hookAllConstructors(mKeyguardBaseClockClass,
                new MethodHookWrapper() {
                    @Override
                    protected void after(MethodHookParam param) {
                        final LinearLayout keyguardClock = (LinearLayout) param.thisObject;
                        modContext = keyguardClock.getContext().getApplicationContext();

                        keyguardClock.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                            @Override
                            public void onWindowAttached() {
                                addClock(keyguardClock);
                            }

                            @Override
                            public void onWindowDetached() {
                                removeClock(keyguardClock);
                            }
                        });
                        addClock(keyguardClock);

                        ScreenBroadcastManager.getInstance(keyguardClock.getContext()).registerListener(screenListener);

                    }
                });
    }

    private synchronized void addClock(View clock) {
        if (!mKeyguardClockList.contains(clock)) {
            mKeyguardClockList.add(clock);

            int size = mKeyguardClockList.size();
            int limitedSize = 2;
            if (size > limitedSize) {
                for (int i = 0; i < size - limitedSize; i++) {
                    View item = mKeyguardClockList.get(i);
                    mKeyguardClockList.remove(item);
                }
            }
        }
    }

    private synchronized void removeClock(View clock) {
        mKeyguardClockList.remove(clock);
    }

    private final SimpleScreenListener screenListener = new SimpleScreenListener() {
        @Override
        public void onScreenOn() {
            loadOneSentence();
        }

        @Override
        public void onUserPresent() {
            cancelLoadOneSentence();
        }

        @Override
        public void onScreenOff() {
            cancelLoadOneSentence();
        }
    };

    private void loadOneSentence() {
        if (modContext != null) {
            OneSentenceManager.getInstance().loadOneSentence(modContext, xsp, new OneSentenceManager.OneSentenceLoadListener() {
                @Override
                public void onSuccess(@NotNull String oneSentence) {
                    showOneSentence(oneSentence);
                }

                @Override
                public void onFailed(@NotNull Throwable throwable) {
                    XLog.e("Error occurs when load OneSentence", throwable);
                }
            });
        }
    }

    private void showOneSentence(String oneSentence) {
        for (View keyguardClock : mKeyguardClockList) {
            try {
                TextView hitokotoInfo = keyguardClock.findViewById(R.id.hitokoto_info_text_view);
                hitokotoInfo.setText(oneSentence);
            } catch (Throwable t) {
                // ignore
                XLog.e("", t);
            }
        }
    }

    private void cancelLoadOneSentence() {
        if (mCompositeDisposable.size() > 0) {
            mCompositeDisposable.clear();
        }
        OneSentenceManager.getInstance().cancelLoadOneSentence();
    }
}
