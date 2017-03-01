import cv

cv.NamedWindow("Camera 1")
cv.NamedWindow("Camera 2")
video1 = cv.CaptureFromCAM(0)
cv.SetCaptureProperty(video1, cv.CV_CAP_PROP_FRAME_WIDTH, 800)
cv.SetCaptureProperty(video1, cv.CV_CAP_PROP_FRAME_HEIGHT, 600)

video2 = cv.CaptureFromCAM(1)
cv.SetCaptureProperty(video2, cv.CV_CAP_PROP_FRAME_WIDTH, 800)
cv.SetCaptureProperty(video2, cv.CV_CAP_PROP_FRAME_HEIGHT, 600)

loop = True
while(loop == True):
    frame1 = cv.QueryFrame(video1)
    frame2 = cv.QueryFrame(video2)
    cv.ShowImage("Camera 1", frame1)
    cv.ShowImage("Camera 2", frame2)
    char = cv.WaitKey(99)
    if (char == 27):
        loop = False

cv.DestroyWindow("Camera 1")
cv.DestroyWindow("Camera 2")