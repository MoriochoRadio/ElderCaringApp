package com.example.caring01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private EditText txtId, txtPw;
    private Button loginBtn;
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtId = findViewById(R.id.txtId);
        txtPw = findViewById(R.id.txtPw);
        loginBtn = findViewById(R.id.btnLogin);
        TextView joinTV = findViewById(R.id.joinTV);

        reference = db.getReference("users");

        // 회원가입 클릭 리스너 설정
        joinTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // JoinActivity로 이동하는 인텐트
                Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });

        // 로그인 버튼 클릭 리스너
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        final String id = txtId.getText().toString().trim();
        final String password = txtPw.getText().toString().trim();

        if (!id.isEmpty() && !password.isEmpty()) {
            // Firebase 데이터베이스에서 아이디로 사용자 검색
            reference.orderByChild("id").equalTo(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // 사용자 데이터를 순회하면서 비밀번호 확인
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String dbPassword = userSnapshot.child("password").getValue(String.class);
                            if (dbPassword != null && dbPassword.equals(password)) {
                                Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                                // 로그인 성공 시 이동할 액티비티로 전환
                                Intent intent = new Intent(LoginActivity.this, MainActivity2.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                        Toast.makeText(LoginActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "해당 ID의 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(LoginActivity.this, "데이터베이스 오류: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
        }
    }
}
