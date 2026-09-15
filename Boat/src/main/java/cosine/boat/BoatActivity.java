package cosine.boat;

import android.content.Context;
import android.os.Handler;
import android.view.TextureView;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Vector;

import cosine.boat.function.BoatCallback;
import cosine.boat.function.BoatLaunchCallback;

public class BoatActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

	private TextureView mainTextureView;
	public BoatCallback boatCallback;
	public float scaleFactor = 1.0F;

	/** 1.0.7：游戏画面已输出通知是否已发出（只发一次） */
	private boolean picOutputNotified = false;

	public void init(){
		nOnCreate();

		mainTextureView = findViewById(R.id.main_surface);
		mainTextureView.setSurfaceTextureListener(this);
	}

	/**
	 * 1.0.7：暴露渲染视图给子类，用于「游戏画面是否已输出」的兜底探测
	 * （见 QCL 侧的 GameFrameProbe）。透明/未就绪时 onSurfaceTextureUpdated
	 * 可能不派发，需要主动采样判断。
	 */
	public TextureView getMainTextureView() {
		return mainTextureView;
	}
	
	public static native void setBoatNativeWindow(Surface surface);

	public native void nOnCreate();
	
	static {
		System.loadLibrary("boat");
	}
	
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		System.out.println("SurfaceTexture is available!");
		boatCallback.onSurfaceTextureAvailable(surface,width,height);
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		boatCallback.onSurfaceTextureSizeChanged(surface,width,height);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// 1.0.7：与 Pojav 侧同款修复。原来用 int output 计数器（output==1 才触发一次），
		// 逻辑绕且易漏。改成 boolean 标志，只通知一次，语义清楚。
		if (!picOutputNotified) {
			picOutputNotified = true;
			android.util.Log.i("jrelog", "[画面切换] 游戏首帧到达（onSurfaceTextureUpdated）");
			boatCallback.onPicOutput();
		}
	}

	/** 1.0.9：时序竞争修复 —— onStart（showBackground）盖回等待界面后清零标志，
	 *  让游戏下一帧重新触发 onPicOutput → hideBackground（见 Pojav 侧同款注释）。 */
	public void resetPicOutputFlag() {
		picOutputNotified = false;
	}

	public void startGame(final String javaPath,final String home,final boolean highVersion,final Vector<String> args,String renderer,String gameDir){
		// 1.0.7：重置「画面已输出」标志 —— 每次启动都要清零，否则上一次启动
		// （或视图预热阶段）可能已经把它置位，导致这次的等待界面再也等不到
		// onPicOutput()，界面就一直卡着。
		picOutputNotified = false;
		Handler handler = new Handler();
		new Thread(() -> LoadMe.launchMinecraft(handler, BoatActivity.this, javaPath, home, highVersion, args, renderer, gameDir, new BoatLaunchCallback() {
			@Override
			public void onStart() {
				boatCallback.onStart();
			}

			@Override
			public void onError(Exception e) {
				boatCallback.onError(e);
			}
		})).start();
	}

	public void setCursorMode(int mode) {
		boatCallback.onCursorModeChange(mode);
	}

	public static void onExit(Context ctx, int code) {
		((BoatActivity) ctx).boatCallback.onExit(code);
	}

	public void setBoatCallback(BoatCallback callback) {
		this.boatCallback = callback;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (mainTextureView != null && mainTextureView.getSurfaceTexture() != null) {
			mainTextureView.post(() -> {
				boatCallback.onSurfaceTextureSizeChanged(mainTextureView.getSurfaceTexture(),mainTextureView.getWidth(),mainTextureView.getHeight());
			});
		}
	}
}



