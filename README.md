# Morsify
<b>An android app for converting alpha numeric text to morse code (also with feedbacks like vibration, sound and flash).</b>
            
•	The app is called Morsify. It is developed in Android Studio 3.5.<br>
•	Minimum Android Version is 6.0 (Marshmallow)<br>
•	There is an Edit Text widget for user-input. The user inputs the text he/she wants to convert to Morse Code here.<br>
•	There are three checkboxes to enable the user to select from three feedback options: i) Vibrations, ii) Sound, and iii) Flashes. 
The user can select one or more (or none) of the feedbacks. The result is that when the user presses the “CONVERT” button, it will generate the selected physical feedbacks from the Morse Text generated. These feedbacks can be used for a variety of use cases.<br>
•	There is Speed slider implemented using Discrete Seek Bar Widget which listens to when the user changes the value. The change is then translated and updates the feedbackSpeed variable, which is a modifier that changes the speed at which the physical feedbacks are outputted. <br>
•	Another Edit Text widget is available to change the frequency at which the Morse Code sound is generated. The default frequency is 600hz. The frequency gets updated dynamically as the user is typing the text.<br>
•	A Text Field widget is placed near the bottom which displays the generated Morse Code in the form of dots and dashes.<br>
•	Below and to  the right of Morse Code Text Field is a Button named “Copy”, which copies the generated morse code to your phone’s clipboard.<br>
•	The bottom most part of the UI houses the “CONVERT” Button. It is used to convert the input text into morse and generate the selected feedbacks. If a feedback thread is already running, the button will be converted to “Stop Feedbacks”, which will terminate the current feedback generation thread. <br>
You can also stop running feedback generation thread by de-selecting the various feedback checkboxes.<br><br>
