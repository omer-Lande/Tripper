# Tripper

Tripper is an Android application designed to help travelers find companions for their journeys. Inspired by the swipe-to-match mechanism popularized by Tinder, Tripper allows users to swipe through potential travel partners based on shared interests, age, and gender preferences.

## Features

- **User Authentication**: Secure login and sign-up using Firebase Authentication.
- **Profile Management**: Users can create and manage their profiles, including uploading profile pictures, specifying interests, and setting travel preferences.
- **Matching System**: Users can swipe right to like or left to pass on potential travel companions. If two users like each other, they become a match.
- **Chat Functionality**: Matched users can chat with each other within the app.
- **Payment Integration**: Users can unlock unlimited swipes by making a payment.

## Tech Stack

- **Android**: Developed using Java.
- **Firebase**: 
  - **Firestore**: For storing user data, including profiles, matches, and chat messages.
  - **Realtime Database**: Used for real-time chat functionality.
  - **Firebase Authentication**: For user sign-up, login, and authentication.
- **Glide**: For efficient image loading.
- **Picasso**: For additional image handling within the app.
- **XML**: For designing the UI components.

## Setup and Installation

**Clone the Repository:**
   ```bash
   git clone https://github.com/yourusername/tripper.git
