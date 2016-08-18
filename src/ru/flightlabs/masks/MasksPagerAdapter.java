package ru.flightlabs.masks;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MasksPagerAdapter extends PagerAdapter {

    FdActivity fdAct;
    Context mContext;
    TypedArray images;
    LayoutInflater mLayoutInflater;

    public MasksPagerAdapter(FdActivity context, TypedArray images) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.images = images;
        this.fdAct = context;
    }

    @Override
    public int getCount() {
        return images.length();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View itemView = mLayoutInflater.inflate(R.layout.item_effect, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.item_image);
        imageView.setImageResource(images.getResourceId(position, 0));
        container.addView(itemView);
        itemView.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                fdAct.changeMask(position);
                
            }
        });

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
    
    @Override
    public float getPageWidth(int position) {
        return 1f / 4;
    }
}