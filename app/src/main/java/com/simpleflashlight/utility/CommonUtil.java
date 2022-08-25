package com.simpleflashlight.utility;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class CommonUtil {
    public static void showDialog(Context context, String title, String message, int iconId, String positiveButton, final AlertDialogOnClickListener listener) {
        if (context == null) {
            return;
        }
        try {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setIcon(iconId);
            alertDialogBuilder.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        if (listener != null) {
                            listener.onClick(dialog, which);
                        }
                        dialog.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

//            if (!isNullOrEmpty(negativeButton)) {
//                alertDialogBuilder.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        try {
//                            if (listener != null) {
//                                listener.onClick(dialog, which);
//                            }
//                            dialog.cancel();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//            }

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface AlertDialogOnClickListener {
        public void onClick(DialogInterface dialog, int which);
    }
}
