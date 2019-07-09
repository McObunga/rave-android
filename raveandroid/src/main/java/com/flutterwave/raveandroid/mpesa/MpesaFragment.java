package com.flutterwave.raveandroid.mpesa;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.flutterwave.raveandroid.Payload;
import com.flutterwave.raveandroid.PayloadBuilder;
import com.flutterwave.raveandroid.R;
import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.Utils;

import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 */
public class MpesaFragment extends Fragment implements MpesaContract.View {

    View fragment;
    TextInputEditText amountEt;
    TextInputLayout amountTil;
    TextInputEditText phoneEt;
    TextInputLayout phoneTil;
    RavePayInitializer ravePayInitializer;
    private ProgressDialog progressDialog;
    private ProgressDialog pollingProgressDialog ;
    MpesaPresenter presenter;
    static int rave_phoneEtInt;

    public MpesaFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        fragment = inflater.inflate(R.layout.fragment_mpesa, container, false);

        presenter = new MpesaPresenter(getActivity(), this);
        amountEt = (TextInputEditText) fragment.findViewById(R.id.rave_amountTV);
        amountTil = (TextInputLayout) fragment.findViewById(R.id.rave_amountTil);
        phoneEt = (TextInputEditText) fragment.findViewById(R.id.rave_phoneEt);
        phoneTil = (TextInputLayout) fragment.findViewById(R.id.rave_phoneTil);

        rave_phoneEtInt = fragment.findViewById(R.id.rave_amountTV).getId();

        Button payButton = (Button) fragment.findViewById(R.id.rave_payButton);
        presenter.validate(fragment);
        ravePayInitializer = ((RavePayActivity) getActivity()).getRavePayInitializer();

        double amountToPay = ravePayInitializer.getAmount();

        if (amountToPay > 0) {
            amountTil.setVisibility(GONE);
            amountEt.setText(String.valueOf(amountToPay));
        }

        return fragment;
    }

    @Override
    public void onPollingRoundComplete(String flwRef, String txRef, String publicKey) {

        if (pollingProgressDialog != null && pollingProgressDialog.isShowing()) {
            presenter.requeryTx(flwRef, txRef, publicKey);
        }

    }

    @Override
    public void showPollingIndicator(boolean active) {
        if (getActivity().isFinishing()) { return; }

        if(pollingProgressDialog == null) {
            pollingProgressDialog = new ProgressDialog(getActivity());
            pollingProgressDialog.setMessage("Checking transaction status. \nPlease wait");
        }

        if (active && !pollingProgressDialog.isShowing()) {
            pollingProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pollingProgressDialog.dismiss();
                }
            });

            pollingProgressDialog.show();
        }
        else if (active && pollingProgressDialog.isShowing()) {
            //pass
        }
        else {
            pollingProgressDialog.dismiss();
        }
    }

    @Override
    public void showProgressIndicator(boolean active) {

        if (getActivity().isFinishing()) { return; }

        if(progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("Please wait...");
        }

        if (active && !progressDialog.isShowing()) {
            progressDialog.show();
        }
        else if (active && progressDialog.isShowing()) {
            //pass
        }
        else {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onPaymentError(String message) {
//        dismissDialog();
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentSuccessful(String status, String flwRef, String responseAsString) {
        Intent intent = new Intent();
        intent.putExtra("response", responseAsString);

        if (getActivity() != null) {
            getActivity().setResult(RavePayActivity.RESULT_SUCCESS, intent);
            getActivity().finish();
        }
    }

    @Override
    public void displayFee(String charge_amount, final Payload payload) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("You will be charged a total of " + charge_amount + ravePayInitializer.getCurrency() + ". Do you want to continue?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();

        presenter.chargeMpesa(payload, ravePayInitializer.getEncryptionKey());


            }
        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    @Override
    public void showFetchFeeFailed(String s) {
        showToast(s);
    }

    @Override
    public void onPaymentFailed(String message, String responseAsJSONString) {
        Intent intent = new Intent();
        intent.putExtra("response", responseAsJSONString);
        if (getActivity() != null) {
            getActivity().setResult(RavePayActivity.RESULT_ERROR, intent);
            getActivity().finish();
        }
    }

    @Override
    public void onValidate(Boolean valid) {

            if(valid){

                String amount = amountEt.getText().toString();
                String phone = phoneEt.getText().toString();

                ravePayInitializer.setAmount(Double.parseDouble(amount));

                String txRef = ravePayInitializer.getTxRef();
                Log.d("txRef", txRef);
                PayloadBuilder builder = new PayloadBuilder();
                builder.setAmount(ravePayInitializer.getAmount() + "")
                        .setCountry(ravePayInitializer.getCountry())
                        .setCurrency(ravePayInitializer.getCurrency())
                        .setEmail(ravePayInitializer.getEmail())
                        .setFirstname(ravePayInitializer.getfName())
                        .setLastname(ravePayInitializer.getlName())
                        .setIP(Utils.getDeviceImei(getActivity()))
                        .setTxRef(ravePayInitializer.getTxRef())
                        .setMeta(ravePayInitializer.getMeta())
                        .setSubAccount(ravePayInitializer.getSubAccount())
                        .setPhonenumber(phone)
                        .setPBFPubKey(ravePayInitializer.getPublicKey())
                        .setIsPreAuth(ravePayInitializer.getIsPreAuth())
                        .setDevice_fingerprint(Utils.getDeviceImei(getActivity()));

                if (ravePayInitializer.getPayment_plan() != null) {
                    builder.setPaymentPlan(ravePayInitializer.getPayment_plan());
                }

                Payload body = builder.createMpesaPayload();

                if(ravePayInitializer.getIsDisplayFee()){
                    presenter.fetchFee(body);
                } else {
                    presenter.chargeMpesa(body, ravePayInitializer.getEncryptionKey());
                }
            }
            else{
                    Log.d("okh", "not valid");

            }
    }
}
