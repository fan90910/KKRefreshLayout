package net.z0kai.kkrefreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import net.z0kai.kkrefreshlayout.footview.horizontal.ArrowFooterView;
import net.z0kai.kkrefreshlayout.headerview.EmptyHeaderView;
import net.z0kai.refreshlayout.KKRefreshLayout;
import net.z0kai.refreshlayout.KKRefreshListener;

public class RecyclerViewHActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_h);

        refreshLayout = (KKRefreshLayout) findViewById(R.id.refreshLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new TestAdapter(this);
        recyclerView.setAdapter(adapter);

        refreshLayout.setRefreshListener(new KKRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.finishRefresh();
                        refreshLayout.finishLoadMore();
                        adapter.refresh();
                    }
                }, 2000);
            }

            @Override
            public void onLoadMore() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.finishLoadMore();
                        refreshLayout.finishRefresh();
//                        adapter.addData();
                        Toast.makeText(RecyclerViewHActivity.this, "on load more", Toast.LENGTH_SHORT).show();
                    }
                }, 2000);
            }
        });
        refreshLayout.setHeaderView(new EmptyHeaderView(this));
        refreshLayout.setFooterView(new ArrowFooterView(this));
    }
}