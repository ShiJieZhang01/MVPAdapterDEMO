package com.looklook.xinghongfei.looklook.Activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.looklook.xinghongfei.looklook.R;
import com.looklook.xinghongfei.looklook.bean.zhihu.ZhihuStory;
import com.looklook.xinghongfei.looklook.config.Config;
import com.looklook.xinghongfei.looklook.presenter.IZhihuStoryPresenter;
import com.looklook.xinghongfei.looklook.presenter.implPresenter.ZhihuStoryPresenterImpl;
import com.looklook.xinghongfei.looklook.presenter.implView.IZhihuStory;
import com.looklook.xinghongfei.looklook.util.AnimUtils;
import com.looklook.xinghongfei.looklook.util.ColorUtils;
import com.looklook.xinghongfei.looklook.util.DensityUtil;
import com.looklook.xinghongfei.looklook.util.GlideUtils;
import com.looklook.xinghongfei.looklook.util.ViewUtils;
import com.looklook.xinghongfei.looklook.util.WebUtil;
import com.looklook.xinghongfei.looklook.widget.ElasticDragDismissFrameLayout;
import com.looklook.xinghongfei.looklook.widget.ParallaxScrimageView;
import com.looklook.xinghongfei.looklook.widget.TranslateYTextView;

import java.lang.reflect.InvocationTargetException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by xinghongfei on 16/8/13.
 */
public class ZhihuDescribeActivity extends BaseActivity implements IZhihuStory {
    private static final float SCRIM_ADJUSTMENT = 0.075f;

    @InjectView(R.id.shot)
    ParallaxScrimageView mShot;
    @InjectView(R.id.toolbar)
    Toolbar mToolbar;
    @InjectView(R.id.wv_zhihu)
    WebView wvZhihu;
    @InjectView(R.id.nest)
    NestedScrollView mNest;
    @InjectView(R.id.title)
    TranslateYTextView mTranslateYTextView;

    boolean isEmpty;
    String mBody;
    String[] scc;
    String mImageUrl;

    @InjectView(R.id.draggable_frame)
    ElasticDragDismissFrameLayout mDraggableFrame;

    int[] mDeviceInfo;
    int width;
    int heigh;
    private Transition.TransitionListener zhihuReturnHomeListener;
    private NestedScrollView.OnScrollChangeListener scrollListener;

    private String id;
    private String title;
    private String url;
    private IZhihuStoryPresenter mIZhihuStoryPresenter;
    private ElasticDragDismissFrameLayout.SystemChromeFader chromeFader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zhihudescribe);
        ButterKnife.inject(this);
        mDeviceInfo = DensityUtil.getDeviceInfo(this);
        width = mDeviceInfo[0];
        heigh = width * 3 / 4;
        setSupportActionBar(mToolbar);
        initlistenr();
        initData();
        initView();
        getData();

        chromeFader = new ElasticDragDismissFrameLayout.SystemChromeFader(this);

        //设置activity的进入动画和退出动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            getWindow().getSharedElementReturnTransition().addListener(zhihuReturnHomeListener);
            getWindow().setSharedElementEnterTransition(new ChangeBounds());
        }

        enterAnimation();
    }

    private void initlistenr() {
        zhihuReturnHomeListener =
                new AnimUtils.TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        super.onTransitionStart(transition);
                        // hide the fab as for some reason it jumps position??  TODO work out why
                        mToolbar.animate()
                                .alpha(0f)
                                .setDuration(100)
                                .setInterpolator(new AccelerateInterpolator());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mShot.setElevation(1f);
                            mToolbar.setElevation(0f);
                        }
                        mNest.animate()
                                .alpha(0f)
                                .setDuration(50)
                                .setInterpolator(new AccelerateInterpolator());
                    }
                };
        scrollListener = new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                //NestedScrollView在起始位置向上滑动，标题和背景图片也会向上滑动一段距离，然后保持不动，NestedScrollView 继续上滑会覆盖它们，去掉此段代码，背景图片和标题保持不动被NestedScrollView覆盖
                if (oldScrollY < 168) {
                    mShot.setOffset(-oldScrollY);
                    mTranslateYTextView.setOffset(-oldScrollY);
                }

            }
        };
    }

    private void initData() {
        id = getIntent().getStringExtra("id");
        title = getIntent().getStringExtra("title");
        mImageUrl = getIntent().getStringExtra("image");
        mIZhihuStoryPresenter = new ZhihuStoryPresenterImpl(this);
        mNest.setOnScrollChangeListener(scrollListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //调用postponeEnterTransition() 方法来暂时阻止启动共享元素 Transition。之后，
            // 在共享元素准备好后调用 startPostponedEnterTransition 来恢复过渡效果
            postponeEnterTransition();
            mShot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mShot.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startPostponedEnterTransition();
                    }
                    return true;
                }
            });
        }
    }

    private void initView() {
        mToolbar.setTitleMargin(20, 20, 0, 10);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        //点击toolbar，界面滑到初始位置
        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNest.smoothScrollTo(0, 0);
            }
        });
        //点击后退箭头退出
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandImageAndFinish();
            }
        });
        mTranslateYTextView.setText(title);
        //webview设置
        WebSettings settings = wvZhihu.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        //settings.setUseWideViewPort(true);造成文字太小
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCachePath(getCacheDir().getAbsolutePath() + "/webViewCache");
        settings.setAppCacheEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        wvZhihu.setWebChromeClient(new WebChromeClient());

    }


    @Override
    protected void onResume() {
        super.onResume();

        mDraggableFrame.addListener(chromeFader);
        try {
            wvZhihu.getClass().getMethod("onResume").invoke(wvZhihu, (Object[]) null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDraggableFrame.removeListener(chromeFader);
        try {
            wvZhihu.getClass().getMethod("onPause").invoke(wvZhihu, (Object[]) null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            getWindow().getSharedElementReturnTransition().removeListener(zhihuReturnHomeListener);
        }
        //webview内存泄露
        if (wvZhihu != null) {
            ((ViewGroup) wvZhihu.getParent()).removeView(wvZhihu);
            wvZhihu.destroy();
            wvZhihu = null;
        }
        mIZhihuStoryPresenter.unsubcrible();
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        expandImageAndFinish();

    }

    private void getData() {
        mIZhihuStoryPresenter.getZhihuStory(id);

    }

    @OnClick(R.id.shot)
    public void onClick() {
        mNest.smoothScrollTo(0, 0);

    }

    @Override
    public void showError(String error) {
        Snackbar.make(wvZhihu, getString(R.string.snack_infor), Snackbar.LENGTH_INDEFINITE).setAction("重试", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();
            }
        }).show();
    }

    @Override
    public void showZhihuStory(ZhihuStory zhihuStory) {
        //加载图片
        Glide.with(this)
                .load(zhihuStory.getImage()).centerCrop()
                .listener(loadListener).override(width, heigh)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(mShot);
        url = zhihuStory.getShareUrl();
        isEmpty = TextUtils.isEmpty(zhihuStory.getBody());
        mBody = zhihuStory.getBody();
        scc = zhihuStory.getCss();
        //获取不到内容的话，则显示固定内容,Config.isNight用来决定是否采用夜间模式
        if (isEmpty) {
            wvZhihu.loadUrl(url);
        } else {
            String data = WebUtil.buildHtmlWithCss(mBody, scc, Config.isNight);
            wvZhihu.loadDataWithBaseURL(WebUtil.BASE_URL, data, WebUtil.MIME_TYPE, WebUtil.ENCODING, WebUtil.FAIL_URL);
        }
    }

    //退出的动画效果
    private void expandImageAndFinish() {
        if (mShot.getOffset() != 0f) {
            Animator expandImage = ObjectAnimator.ofFloat(mShot, ParallaxScrimageView.OFFSET,
                    0f);
            expandImage.setDuration(80);
            expandImage.setInterpolator(new AccelerateInterpolator());
            expandImage.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAfterTransition();
                    } else {
                        finish();
                    }
                }
            });
            expandImage.start();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAfterTransition();
            } else {
                finish();
            }
        }
    }

    private RequestListener loadListener = new RequestListener<String, GlideDrawable>() {
        @Override
        public boolean onResourceReady(GlideDrawable resource, String model,
                                       Target<GlideDrawable> target, boolean isFromMemoryCache,
                                       boolean isFirstResource) {
            final Bitmap bitmap = GlideUtils.getBitmap(resource);
            final int twentyFourDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24, ZhihuDescribeActivity.this.getResources().getDisplayMetrics());

            //通过加载图片的颜色，动态调整状态栏的背景颜色和文字颜色
            Palette.from(bitmap)
                    .maximumColorCount(3)
                    .clearFilters() /* by default palette ignore certain hues
                        (e.g. pure black/white) but we don't want this. */
                    .setRegion(0, 0, bitmap.getWidth() - 1, twentyFourDip) /* - 1 to work around
                        https://code.google.com/p/android/issues/detail?id=191013 */
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            boolean isDark;
                            @ColorUtils.Lightness int lightness = ColorUtils.isDark(palette);
                            if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                                isDark = ColorUtils.isDark(bitmap, bitmap.getWidth() / 2, 0);
                            } else {
                                isDark = lightness == ColorUtils.IS_DARK;
                            }

                            // color the status bar. Set a complementary dark color on L,
                            // light or dark color on M (with matching status bar icons)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {


                                int statusBarColor = getWindow().getStatusBarColor();
                                final Palette.Swatch topColor =
                                        ColorUtils.getMostPopulousSwatch(palette);
                                if (topColor != null &&
                                        (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                                    statusBarColor = ColorUtils.scrimify(topColor.getRgb(),
                                            isDark, SCRIM_ADJUSTMENT);
                                    // set a light status bar on M+
                                    if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        ViewUtils.setLightStatusBar(mShot);
                                    }

                                }

                                if (statusBarColor != getWindow().getStatusBarColor()) {
                                    mShot.setScrimColor(statusBarColor);
                                    ValueAnimator statusBarColorAnim = ValueAnimator.ofArgb(
                                            getWindow().getStatusBarColor(), statusBarColor);
                                    statusBarColorAnim.addUpdateListener(new ValueAnimator
                                            .AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            getWindow().setStatusBarColor(
                                                    (int) animation.getAnimatedValue());
                                        }
                                    });
                                    statusBarColorAnim.setDuration(1000L);
                                    statusBarColorAnim.setInterpolator(
                                            new AccelerateInterpolator());
                                    statusBarColorAnim.start();
                                }
                            }
                        }
                    });


            Palette.from(bitmap)
                    .clearFilters()
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {

                            // slightly more opaque ripple on the pinned image to compensate
                            // for the scrim
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                                mShot.setForeground(ViewUtils.createRipple(palette, 0.3f, 0.6f,
                                        ContextCompat.getColor(ZhihuDescribeActivity.this, R.color.mid_grey),
                                        true));
                            }
                        }
                    });

            // TODO should keep the background if the image contains transparency?!
            mShot.setBackground(null);
            return false;
        }

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                   boolean isFirstResource) {
            return false;
        }
    };

    //进入的动画效果，图片和文字出现的过渡时间和透明度
    private void enterAnimation() {
        float offSet = mToolbar.getHeight();
        LinearInterpolator interpolator = new LinearInterpolator();
        viewEnterAnimation(mShot, offSet, interpolator);
        viewEnterAnimationNest(mNest, 0f, interpolator);

    }

    private void viewEnterAnimation(View view, float offset, Interpolator interp) {
        view.setTranslationY(-offset);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500L)
                .setInterpolator(interp)
                .setListener(null)
                .start();
    }

    private void viewEnterAnimationNest(View view, float offset, Interpolator interp) {
        view.setTranslationY(-offset);
        view.setAlpha(0.3f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500L)
                .setInterpolator(interp)
                .setListener(null)
                .start();
    }

}
