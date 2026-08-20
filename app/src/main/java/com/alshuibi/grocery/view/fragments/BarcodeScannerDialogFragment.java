package com.alshuibi.grocery.view.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.utils.FeedbackManager;

import java.util.Arrays;
import java.util.List;

/**
 * ماسح باركود مدمج داخل التطبيق بدل فتح شاشة مسح خارجية.
 * يدعم أشهر باركودات المنتجات ويحتوي إدخالاً يدوياً كخيار احتياطي.
 */
public class BarcodeScannerDialogFragment extends DialogFragment {

    public static final String RESULT_BARCODE = "barcode";
    private static final String ARG_REQUEST_KEY = "request_key";

    private DecoratedBarcodeView barcodeView;
    private EditText manualBarcode;
    private boolean torchOn = false;
    private boolean delivered = false;

    public static BarcodeScannerDialogFragment newInstance(String requestKey) {
        BarcodeScannerDialogFragment fragment = new BarcodeScannerDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startCamera();
                        } else if (isAdded()) {
                            Toast.makeText(
                                    requireContext(),
                                    "يلزم السماح للكاميرا لمسح الباركود، ويمكنك إدخاله يدويًا.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_barcode_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barcodeView = view.findViewById(R.id.barcodeScannerView);
        manualBarcode = view.findViewById(R.id.scannerManualBarcode);
        ImageButton close = view.findViewById(R.id.scannerCloseButton);
        ImageButton torch = view.findViewById(R.id.scannerTorchButton);
        Button useManual = view.findViewById(R.id.scannerUseManualButton);

        List<BarcodeFormat> formats = Arrays.asList(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR
        );

        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.getBarcodeView().getCameraSettings().setAutoFocusEnabled(true);
        barcodeView.setStatusText("قرّب الباركود حتى يصبح واضحًا داخل الإطار");

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result == null || result.getText() == null) return;
                deliverResult(result.getText());
            }

            @Override
            public void possibleResultPoints(List<com.google.zxing.ResultPoint> resultPoints) {
                // لا نحتاج نقاط المعاينة في هذه الواجهة.
            }
        });

        close.setOnClickListener(v -> dismiss());

        torch.setOnClickListener(v -> {
            torchOn = !torchOn;
            if (torchOn) {
                barcodeView.setTorchOn();
            } else {
                barcodeView.setTorchOff();
            }
            torch.setAlpha(torchOn ? 1f : 0.55f);
        });

        useManual.setOnClickListener(v -> {
            String value = manualBarcode.getText().toString().trim();
            if (value.isEmpty()) {
                manualBarcode.setError("أدخل رقم الباركود");
                return;
            }
            deliverResult(value);
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void deliverResult(String rawValue) {
        if (delivered || rawValue == null) return;
        String value = rawValue.trim();
        if (value.isEmpty()) return;

        delivered = true;
        if (barcodeView != null) barcodeView.pause();
        if (isAdded()) FeedbackManager.success(requireContext());

        String requestKey = getArguments() == null
                ? "barcode_result"
                : getArguments().getString(ARG_REQUEST_KEY, "barcode_result");

        Bundle data = new Bundle();
        data.putString(RESULT_BARCODE, value);
        getParentFragmentManager().setFragmentResult(requestKey, data);
        dismissAllowingStateLoss();
    }

    private void startCamera() {
        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    public void onResume() {
        super.onResume();

        Window window = getDialog() == null ? null : getDialog().getWindow();
        if (window != null) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.94f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.60f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (barcodeView != null
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    public void onPause() {
        if (barcodeView != null) barcodeView.pause();
        super.onPause();
    }
}
