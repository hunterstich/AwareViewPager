# AwareViewPager
A ViewPager with a parallax header, transforming toolbar and tabs

## Description
This is an example of a ViewPager with a header image, a title box, tabs and a fab.  Those 
views make up the expanded toolbar, which tranlate and transform into a compact toolbar on scrolling of 
the ViewPager.  The ViewPager's fragments watch eachother and update their RecyclerViews 
scroll positions so that when the ViewPager is scrolled, the adjacent RecyclerView is in the correct position.

## Preview

Videos: https://www.youtube.com/watch?v=QqP0todMHnk https://www.youtube.com/watch?v=kCNN7tVDUKc

![Alt text](https://github.com/hunterrobbert/AwareViewPager/blob/master/preview.png "Optional title")

## Notes & Issues

The way this works is basically by observing the RecyclerView scroll and a bit of math to translate view in the 
header accordingly.  The important variables the math relys on are in the dimens.xml and used throughout layout 
files and related classes.  This makes the whole system seem quite fragile.  For example, going into the examples activity_main.xml
and manually changing the header_box height without reflecting that change or directly doing so in dimens.xml will
most likely cause some strange scroll behavior.  

Initially there was an issue when a RecyclerView didn't have enough items to be scrolled to the top of the screen.  If 
ViewPager frag1 was scrolled to the top and then the user scrolled to frag2, the header would jump down to the 
correct spot. This was solved by giving the adapter a footer which sized itself acording to the difference 
between the screen height and the total height of all items in the adapter. It seems to work ok, but occasionally the footer 
will be largely oversized allowing too much scrolling.  


