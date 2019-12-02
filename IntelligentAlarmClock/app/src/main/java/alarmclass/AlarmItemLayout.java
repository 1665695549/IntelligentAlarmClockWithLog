package alarmclass;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.intelligentalarmclock.LogInfo;

import alarmclass.AlarmActivity;


public class AlarmItemLayout extends FrameLayout {

    public AlarmItemLayout(Context context){
        super(context);
    }

    public AlarmItemLayout(Context context, AttributeSet attrs){
        super(context,attrs);
    }

    public AlarmItemLayout(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs,defStyleAttr);
    }

    private static boolean ishorizontal=false;
    int lastX;
    int lastY;
    int offsetX;
    int offsetY;
    int itemwidth;
    int buttonWidth;
    ViewGroup viewGroup;
    View textView;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //LogInfo.d("onTouchEvent: ");
        int x = (int) event.getX();
        int y = (int) event.getY();
        //LogInfo.d("x="+String.valueOf(x)+"y="+String.valueOf(y));

        switch ( event.getAction()){
            case MotionEvent.ACTION_DOWN: {
                //LogInfo.d( "ACTION_DOWN: ");
                AlarmActivity.rerecyclerView.setInterceptTouch(false);//禁止父view拦截事件
                lastX = x;
                lastY = y;
                //MainActivity.rerecyclerView.requestDisallowInterceptTouchEvent(true);//禁止父view截取事件，事件由TestView消费
                //删除按钮显示出来时，直接复位
                if (AlarmActivity.isDeleteIconShown){
                    //LogInfo.d("back star");
                    //MainActivity.current == this
                    if (AlarmActivity.current == this){
                        for (int i=0; i<buttonWidth/600;i++){
                            layout((getLeft() + offsetX), getTop(), (getRight() + offsetX), getBottom());
                        }
                        layout(0, getTop(), itemwidth, getBottom());
                        AlarmActivity.isDeleteIconShown=false;
                        AlarmActivity.rerecyclerView.setInterceptTouch(true);
                        //AlarmActivity.current=null;
                    }
                    //AlarmActivity.rerecyclerView.setInterceptTouch(false);
                    //return false;//表示当前的view不消费此事件，父view消费，接下来的事件此view也不能消费
                }else {
                    AlarmActivity.current=this;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                //LogInfo.d( "ACTION_MOVE: ");
                if (this==AlarmActivity.current){
                    offsetX =x-lastX;
                    offsetY =Math.abs(y-lastY);
                    //LogInfo.d("offsetX="+String.valueOf(offsetX));
                    //LogInfo.d( "offsetY="+String.valueOf(offsetY));
                    itemwidth=getWidth();//全长
                    viewGroup = (ViewGroup) getParent();
                    textView=getChildAt(2);
                    buttonWidth = (viewGroup.getChildAt(0).getWidth()+viewGroup.getChildAt(1).getWidth()); //按钮长度
                   // LogInfo.d("buttonWidth="+String.valueOf(buttonWidth)+"itemwidth="
                   //         +String.valueOf(itemwidth));

                    /*
                     * 若是左右滑动，则是当前view消费事件，return true；否则是上下滑事件（还要消除是偶然的上下滑事件）由父view 消费事件，且把控件移动到正确位置，且让父view拦截接下来的动作
                     */
                    if( Math.abs(offsetX) > Math.abs(offsetY) && Math.abs(offsetX)>buttonWidth/18){
                        //LogInfo.d( "move horizontal");
                        ishorizontal=true;
                        if (getRight()+offsetX < itemwidth && getRight()+offsetX >= itemwidth-(3*buttonWidth)/2) {//滑动效果
                            layout((getLeft() + offsetX), getTop(), (getRight() + offsetX), getBottom());
                        }
                        return true;
                    }else if (Math.abs(offsetX) < Math.abs(offsetY) && offsetY>buttonWidth/18)
                    {
                        if (ishorizontal==false){
                            //LogInfo.d("move vertical");
                            //LogInfo.d( "setInterceptTouch(true)");
                            AlarmActivity.rerecyclerView.setInterceptTouch(true);
                        }

                    }

                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                //LogInfo.d( "ACTION_UP: ");
                //抬手时，滑动距离小于删除控件长度的1/2时，自动向右复位
                if (this==AlarmActivity.current){
                    ishorizontal=false;
                    if (getRight() <= itemwidth && getRight() >= itemwidth - buttonWidth/2)
                    {
                        //LogInfo.d("*****back star");
                        offsetX=itemwidth-getRight();
                        //代替ObjectAnimator.ofFloat方法实现滑动效果
                        for (int i=0; i<offsetX/600;i++){
                            layout((getLeft() + offsetX), getTop(), (getRight() + offsetX), getBottom());
                        }
                        layout(0, getTop(), itemwidth, getBottom());

                    }else if (getRight() < itemwidth-(buttonWidth/2))//抬手时，滑动距离大于删除控件长度的1/2时，显示出删除控件
                    {

                        //LogInfo.d("*****show delete");
                        offsetX=getRight()-(itemwidth-buttonWidth);
                        //代替ObjectAnimator.ofFloat方法实现滑动效果
                        for (int i=0; i<offsetX/600;i++){
                            layout((getLeft() + offsetX), getTop(), (getRight() + offsetX), getBottom());
                        }
                        layout(0-buttonWidth, getTop(), itemwidth-buttonWidth, getBottom());

                        AlarmActivity.isDeleteIconShown=true;
                        AlarmActivity.current=this;
                    }
                }

                break;
            }
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((true==AlarmActivity.isDeleteIconShown) && (this!=AlarmActivity.current)){
            ev.setAction(MotionEvent.ACTION_DOWN);
            AlarmActivity.current.onTouchEvent(ev);
            return true;
        }else {
            return super.onInterceptTouchEvent(ev);
        }
    }
}
