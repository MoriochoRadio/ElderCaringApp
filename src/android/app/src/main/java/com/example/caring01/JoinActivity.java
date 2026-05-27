//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.example.caring01;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Iterator;

public class JoinActivity extends AppCompatActivity {
    private EditText txtId;
    private EditText txtPw;
    private EditText txtPhone;
    private EditText txtCode;
    private Button signBtn;
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference reference;

    public JoinActivity() {}

    @SuppressLint({"MissingInflatedId"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_join);
        this.reference = FirebaseDatabase.getInstance().getReference();
        this.txtId = (EditText) this.findViewById(R.id.txtId);
        this.txtPw = (EditText) this.findViewById(R.id.txtPw);
        this.txtPhone = (EditText) this.findViewById(R.id.txtPhone);
        this.txtCode = (EditText) this.findViewById(R.id.txtCode);
        this.signBtn = (Button) this.findViewById(R.id.signBtn);
        this.signBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                JoinActivity.this.saveUserData();
            }
        });
    }

    private void saveUserData() {
        final String id = this.txtId.getText().toString().trim();
        final String password = this.txtPw.getText().toString().trim();
        final String phone = this.txtPhone.getText().toString().trim();
        final String code = this.txtCode.getText().toString().trim();

        if (!id.isEmpty() && !password.isEmpty() && !phone.isEmpty() && !code.isEmpty()) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("USER");

            // "uid"가 입력한 code 값과 일치하는지 확인
            databaseReference.orderByChild("uid").equalTo(code).addListenerForSingleValueEvent(new ValueEventListener() {
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // 일치하는 uid가 있으면 회원가입 진행
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            String sname = childSnapshot.child("sname").getValue(String.class);
                            User user = new User(id, password, phone, code);
                            JoinActivity.this.reference.child("users").push().setValue(user).addOnCompleteListener((task) -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(JoinActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(JoinActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(JoinActivity.this, "입력한 코드에 해당하는 데이터가 없습니다. 회원가입이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(JoinActivity.this, "데이터베이스 오류: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show();
        }
    }

    public static class User {
        public String id;
        public String password;
        public String phone;
        public String code;

        public User() {}

        public User(String id, String password, String phone, String code) {
            this.id = id;
            this.password = password;
            this.phone = phone;
            this.code = code;
        }
    }
}