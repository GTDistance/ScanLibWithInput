package com.thomas.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

public class ConfirmDialog extends Dialog {

    private Context context;
    private ClickListenerInterface clickListenerInterface;
    private EditText editText;

    public interface ClickListenerInterface {

        public void doConfirm(String editStr);

        public void doCancel();
    }

    public ConfirmDialog(Context context) {
//        super(context,  R.style.custom_dialog_style);
        super(context,context.getResources().getIdentifier("custom_dialog_style", "style", context.getPackageName()));
        this.context = context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        init();
    }
    private int getId(String idName, String type) {
        return this.context.getResources().getIdentifier(idName, type, this.context.getPackageName());
    }
    public void init() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(getId("confirm_dialog","layout"), null);
        setContentView(view);

        view.findViewById(getId("confirm","id")).setOnClickListener(new clickListener());
        view.findViewById(getId("cancel","id")).setOnClickListener(new clickListener());
        editText = (EditText) view.findViewById(getId("edit_text","id"));
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        DisplayMetrics d = context.getResources().getDisplayMetrics(); // 获取屏幕宽、高用
        lp.width = (int) (d.widthPixels * 0.8); // 宽度设置为屏幕的0.8
        dialogWindow.setAttributes(lp);
    }

    public void setClicklistener(ClickListenerInterface clickListenerInterface) {
        this.clickListenerInterface = clickListenerInterface;
    }

    private class clickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int id = v.getId();
            if (id == getId("confirm","id")) {
                String editStr = editText.getText().toString();
                clickListenerInterface.doConfirm(editStr);

            } else if (id == getId("cancel","id")) {
                clickListenerInterface.doCancel();
            }
        }
    };

}