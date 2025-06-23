//export const flagedmockData = [
//    "flagged_posts": [
//        {
//            "post_id": "post_12345",
//            "flag_reason": "potential_crisis",
//            "flagged_at": "2024-01-15T10:30:00Z",
//            "ai_confidence": 0.95,
//            "content_preview": "I can't take this anymore...",
//            "status": "pending_review"
//        }
//    ],
//    "total_pending": 3
//];

export const flagedmockData = {
    "flagged_posts": [
        {
            "post_id": "post_12345",
            "flag_reason": "potential_crisis",
            "flagged_at": "2024-01-15T10:30:00Z",
            "ai_confidence": 0.95,
            "content_preview": "I can't take this anymore...",
            "status": "pending_review"
        }
    ],
    "total_pending": 3
};

export const systemmonitoringmockData = {
        "status": "healthy",
        "services": {
          "database": "down",
          "redis": "up",
          "ai_service": "up"
        },
        "metrics": {
          "active_users": 1247,
          "posts_today": 89,
          "ai_response_time_avg": "450ms"
        }
}

export const mockpostdata = {
  posts:[
  {
    "id": "a84c2f32-1b88-4f73-a901-2de01d857b0e",
    "user": {
      "id": "user-934",
      "emailHash": "f3d6c4b58efb10c7"
    },
    "content": "This is a sample user post, containing some emotional context. I'm feeling really happy today and wanted to share my joy with everyone. The weather is beautiful and I just had the best coffee ever!",
    "emotionTag": "joy",
    "createdAt": "2025-01-22T14:32:00Z"
  },
  {
    "id": "bf34dd4f-25f3-4d0b-b8e5-ff2c3e6dbe64",
    "user": {
      "id": "user-102",
      "emailHash": "adc137f47d8735ae"
    },
    "content": "Feeling overwhelmed today, just needed to share. Everything seems to be going wrong and I don't know how to handle it all. Maybe tomorrow will be better.",
    "emotionTag": "sadness",
    "createdAt": "2025-01-21T09:14:00Z"
  },
  {
    "id": "c45e7f89-3a12-4c56-9d23-8f9a1b2c3d4e",
    "user": {
      "id": "user-567",
      "emailHash": "b7e9f2a1c8d3e4f5"
    },
    "content": "I'm so angry right now! Someone just cut me off in traffic and almost caused an accident. People need to learn how to drive properly!",
    "emotionTag": "anger",
    "createdAt": "2025-01-20T16:45:00Z"
  },
  {
    "id": "d67f8a90-4b23-5d67-0e34-9g0b1c2d3e4f",
    "user": {
      "id": "user-789",
      "emailHash": "c8f0a3b2d9e4f5g6"
    },
    "content": "I'm scared about my upcoming presentation. What if I mess up in front of everyone? I keep having nightmares about it.",
    "emotionTag": "fear",
    "createdAt": "2025-01-19T11:20:00Z"
  },
  {
    "id": "e78g9b01-5c34-6e78-1f45-0h1c2d3e4f5g",
    "user": {
      "id": "user-234",
      "emailHash": "d9g1b4c3e0f5g6h7"
    },
    "content": "Wow! I just found out I got the job I've been dreaming about! I can't believe it actually happened! This is amazing!",
    "emotionTag": "surprise",
    "createdAt": "2025-01-18T13:55:00Z"
  },
  {
    "id": "f89h0c12-6d45-7f89-2g56-1i2d3e4f5g6h",
    "user": {
      "id": "user-456",
      "emailHash": "e0h2c5d4f1g6h7i8"
    },
    "content": "I can't believe people actually enjoy that movie. The acting was terrible and the plot made no sense. What a waste of time.",
    "emotionTag": "disgust",
    "createdAt": "2025-01-17T20:30:00Z"
  },
  {
    "id": "g90i1d23-7e56-8g90-3h67-2j3e4f5g6h7i",
    "user": {
      "id": "user-890",
      "emailHash": "f1i3d6e5g2h7i8j9"
    },
    "content": "Just finished my morning workout and I'm feeling energized! Ready to tackle the day ahead with a positive mindset.",
    "emotionTag": "joy",
    "createdAt": "2025-01-16T07:15:00Z"
  },
  {
    "id": "h01j2e34-8f67-9h01-4i78-3k4f5g6h7i8j",
    "user": {
      "id": "user-123",
      "emailHash": "g2j4e7f6h3i8j9k0"
    },
    "content": "Missing my family today. Wish I could be with them right now. Sometimes being far away is really hard.",
    "emotionTag": "sadness",
    "createdAt": "2025-01-15T18:40:00Z"
  },
  {
    "id": "i12k3f45-9g78-0i12-5j89-4l5g6h7i8j9k",
    "user": {
      "id": "user-345",
      "emailHash": "h3k5f8g7i4j9k0l1"
    },
    "content": "This customer service is absolutely ridiculous! I've been on hold for 45 minutes and they still haven't helped me!",
    "emotionTag": "anger",
    "createdAt": "2025-01-14T12:25:00Z"
  },
  {
    "id": "j23l4g56-0h89-1j23-6k90-5m6h7i8j9k0l",
    "user": {
      "id": "user-678",
      "emailHash": "i4l6g9h8j5k0l1m2"
    },
    "content": "I'm worried about my health test results. The doctor said they'll call tomorrow and I'm really anxious about it.",
    "emotionTag": "fear",
    "createdAt": "2025-01-13T15:10:00Z"
  },
  {
    "id": "k34m5h67-1i90-2k34-7l01-6n7i8j9k0l1m",
    "user": {
      "id": "user-901",
      "emailHash": "j5m7h0i9k6l1m2n3"
    },
    "content": "OMG! My favorite band just announced they're coming to town! I never thought this would happen! I'm so excited!",
    "emotionTag": "surprise",
    "createdAt": "2025-01-12T10:05:00Z"
  },
  {
    "id": "l45n6i78-2j01-3l45-8m12-7o8j9k0l1m2n",
    "user": {
      "id": "user-234",
      "emailHash": "k6n8i1j0l7m2n3o4"
    },
    "content": "That restaurant was disgusting. The food was cold, the service was terrible, and the place was filthy. Never going back there again.",
    "emotionTag": "disgust",
    "createdAt": "2025-01-11T19:50:00Z"
  },
  {
    "id": "m56o7i89-3k12-4m56-9n23-8p7i8j9k0l1m2n",
    "user": {
      "id": "user-567",
      "emailHash": "l7o9i2j1k7l2m3n4o5"
    },
    "content": "Just adopted the cutest puppy today! She's so playful and loving. My heart is full of joy and I can't stop smiling!",
    "emotionTag": "joy",
    "createdAt": "2025-01-10T16:30:00Z"
  },
  {
    "id": "n67p8j90-4l23-5n67-0o34-9q8j9k0l1m2n3o",
    "user": {
      "id": "user-890",
      "emailHash": "m8p0j3k2l8m3n4o5p6"
    },
    "content": "Lost my job today. I don't know how I'm going to pay the bills. Feeling completely hopeless and defeated.",
    "emotionTag": "sadness",
    "createdAt": "2025-01-09T11:45:00Z"
  },
  {
    "id": "o78q9k01-5m34-6o78-1p45-0r9k0l1m2n3o4p",
    "user": {
      "id": "user-123",
      "emailHash": "n9q1k4l3m9n4o5p6q7"
    },
    "content": "My neighbor's dog won't stop barking at 3 AM! I'm so furious! I'm going to call the police if this doesn't stop!",
    "emotionTag": "anger",
    "createdAt": "2025-01-08T03:15:00Z"
  },
  {
    "id": "p89r0l12-6n45-7p89-2q56-1s0l1m2n3o4p5q",
    "user": {
      "id": "user-456",
      "emailHash": "o0r2l5m4n0o5p6q7r8"
    },
    "content": "I heard a strange noise outside my window. I'm terrified someone is trying to break in. I can't sleep now.",
    "emotionTag": "fear",
    "createdAt": "2025-01-07T22:30:00Z"
  },
  {
    "id": "q90s1m23-7o56-8q90-3r67-2t1m2n3o4p5q6r",
    "user": {
      "id": "user-789",
      "emailHash": "p1s3m6n5o1p6q7r8s9"
    },
    "content": "My best friend just proposed to me! I had no idea this was coming! I'm completely shocked and overjoyed!",
    "emotionTag": "surprise",
    "createdAt": "2025-01-06T18:20:00Z"
  },
  {
    "id": "r01t2n34-8p67-9r01-4s78-3u2n3o4p5q6r7s",
    "user": {
      "id": "user-012",
      "emailHash": "q2t4n7o6p2q7r8s9t0"
    },
    "content": "The public bathroom was absolutely revolting. I felt like I needed a shower just from being in there. Disgusting!",
    "emotionTag": "disgust",
    "createdAt": "2025-01-05T14:10:00Z"
  },
  {
    "id": "s12u3o45-9q78-0s12-5t89-4v3o4p5q6r7s8t",
    "user": {
      "id": "user-345",
      "emailHash": "r3u5o8p7q3r8s9t0u1"
    },
    "content": "Finished my final exam and I think I aced it! Feeling on top of the world right now! All that studying paid off!",
    "emotionTag": "joy",
    "createdAt": "2025-01-04T12:00:00Z"
  },
  {
    "id": "t23v4p56-0r89-1t23-6u90-5w4p5q6r7s8t9u",
    "user": {
      "id": "user-678",
      "emailHash": "s4v6p9q8r4s9t0u1v2"
    },
    "content": "My grandmother passed away today. I'm heartbroken and don't know how to process this loss. She was everything to me.",
    "emotionTag": "sadness",
    "createdAt": "2025-01-03T09:30:00Z"
  },
  {
    "id": "u34w5q67-1s90-2u34-7v01-6x5q6r7s8t9u0v",
    "user": {
      "id": "user-901",
      "emailHash": "t5w7q0r9s5t0u1v2w3"
    },
    "content": "Someone stole my parking spot right in front of me! I was signaling and everything! I'm absolutely livid!",
    "emotionTag": "anger",
    "createdAt": "2025-01-02T17:45:00Z"
  },
  {
    "id": "v45x6r78-2t01-3v45-8w12-7y6r7s8t9u0v1w",
    "user": {
      "id": "user-234",
      "emailHash": "u6x8r1s0t6u1v2w3x4"
    },
    "content": "I think I'm being followed. There's a car that's been behind me for the last 20 minutes. I'm really scared.",
    "emotionTag": "fear",
    "createdAt": "2025-01-01T23:15:00Z"
  },
  {
    "id": "w56y7s89-3u12-4w56-9x23-8z7s8t9u0v1w2x",
    "user": {
      "id": "user-567",
      "emailHash": "v7y9s2t1u7v2w3x4y5"
    },
    "content": "My lottery ticket just won $10,000! I can't believe my luck! This is going to change everything!",
    "emotionTag": "surprise",
    "createdAt": "2024-12-31T20:00:00Z"
  },
  {
    "id": "x67z8t90-4v23-5x67-0y34-9a8t9u0v1w2x3y",
    "user": {
      "id": "user-890",
      "emailHash": "w8z0t3u2v8w3x4y5z6"
    },
    "content": "The food at that restaurant was so bad it made me physically ill. I'm never eating there again!",
    "emotionTag": "disgust",
    "createdAt": "2024-12-30T19:30:00Z"
  },
  {
    "id": "y78a9u01-5w34-6y78-1z45-0b9u0v1w2x3y4z",
    "user": {
      "id": "user-123",
      "emailHash": "x9a1u4v3w9x4y5z6a7"
    },
    "content": "Just got promoted at work! My boss said I'm the best employee they've ever had. I'm so proud of myself!",
    "emotionTag": "joy",
    "createdAt": "2024-12-29T15:20:00Z"
  },
  {
    "id": "z89b0v12-6x45-7z89-2a56-1c0v1w2x3y4z5a",
    "user": {
      "id": "user-456",
      "emailHash": "y0b2v5w4x0y5z6a7b8"
    },
    "content": "My relationship just ended. I thought we were going to get married. I feel like my whole world is falling apart.",
    "emotionTag": "sadness",
    "createdAt": "2024-12-28T13:45:00Z"
  },
  {
    "id": "a90c1w23-7y56-8a90-3b67-2d1w2x3y4z5a6b",
    "user": {
      "id": "user-789",
      "emailHash": "z1c3w6x5y1z6a7b8c9"
    },
    "content": "My computer just crashed and I lost 3 hours of work! I'm so angry I could throw this thing out the window!",
    "emotionTag": "anger",
    "createdAt": "2024-12-27T11:10:00Z"
  },
  {
    "id": "b01d2x34-8z67-9b01-4c78-3e2x3y4z5a6b7c",
    "user": {
      "id": "user-012",
      "emailHash": "a2d4x7y6z2a7b8c9d0"
    },
    "content": "I'm afraid I'm going to fail this class. The final is tomorrow and I don't feel prepared at all.",
    "emotionTag": "fear",
    "createdAt": "2024-12-26T08:30:00Z"
  },
  {
    "id": "c12e3y45-9a78-0c12-5d89-4f3y4z5a6b7c8d",
    "user": {
      "id": "user-345",
      "emailHash": "b3e5y8z7a3b8c9d0e1"
    },
    "content": "My parents just told me they're getting divorced. I had no idea they were having problems. I'm completely shocked.",
    "emotionTag": "surprise",
    "createdAt": "2024-12-25T16:15:00Z"
  },
  {
    "id": "d23f4z56-0b89-1d23-6e90-5g4z5a6b7c8d9e",
    "user": {
      "id": "user-678",
      "emailHash": "c4f6z9a8b4c9d0e1f2"
    },
    "content": "The gym locker room was absolutely filthy. There was mold everywhere and it smelled like something died in there.",
    "emotionTag": "disgust",
    "createdAt": "2024-12-24T14:45:00Z"
  },
  {
    "id": "e34g5a67-1c90-2e34-7f01-6h5a6b7c8d9e0f",
    "user": {
      "id": "user-901",
      "emailHash": "d5g7a0b9c5d0e1f2g3"
    },
    "content": "Just finished running my first marathon! I can't believe I actually did it! I'm so happy and proud of myself!",
    "emotionTag": "joy",
    "createdAt": "2024-12-23T12:00:00Z"
  },
  {
    "id": "f45h6b78-2d01-3f45-8g12-7i6b7c8d9e0f1g",
    "user": {
      "id": "user-234",
      "emailHash": "e6h8b1c0d6e1f2g3h4"
    },
    "content": "My best friend moved to another country today. I'm going to miss them so much. I feel so lonely already.",
    "emotionTag": "sadness",
    "createdAt": "2024-12-22T10:30:00Z"
  },
  {
    "id": "g56i7c89-3e12-4g56-9h23-8j7c8d9e0f1g2h",
    "user": {
      "id": "user-567",
      "emailHash": "f7i9c2d1e7f2g3h4i5"
    },
    "content": "My flight got cancelled and the airline won't give me a refund! I'm absolutely furious about this!",
    "emotionTag": "anger",
    "createdAt": "2024-12-21T09:15:00Z"
  },
  {
    "id": "h67j8d90-4f23-5h67-0i34-9k8d9e0f1g2h3i",
    "user": {
      "id": "user-890",
      "emailHash": "g8j0d3e2f8g3h4i5j6"
    },
    "content": "I think I left the stove on when I left the house. I'm really worried about a fire. Should I go back?",
    "emotionTag": "fear",
    "createdAt": "2024-12-20T07:45:00Z"
  },
  {
    "id": "i78k9e01-5g34-6i78-1j45-0l9e0f1g2h3i4j",
    "user": {
      "id": "user-123",
      "emailHash": "h9k1e4f3g9h4i5j6k7"
    },
    "content": "My sister just had twins! I'm going to be an aunt! I'm so excited and surprised - she didn't know she was having twins!",
    "emotionTag": "surprise",
    "createdAt": "2024-12-19T06:20:00Z"
  },
  {
    "id": "j89l0f12-6h45-7j89-2k56-1m0f1g2h3i4j5k",
    "user": {
      "id": "user-456",
      "emailHash": "i0l2f5g4h0i5j6k7l8"
    },
    "content": "The public pool was absolutely disgusting. There was hair everywhere and the water looked like it hadn't been changed in weeks.",
    "emotionTag": "disgust",
    "createdAt": "2024-12-18T05:10:00Z"
  },
  {
    "id": "k90m1g23-7i56-8k90-3l67-2n1g2h3i4j5k6l",
    "user": {
      "id": "user-789",
      "emailHash": "j1m3g6h5i1j6k7l8m9"
    },
    "content": "Just got accepted into my dream university! I'm so happy and excited about this new chapter in my life!",
    "emotionTag": "joy",
    "createdAt": "2024-12-17T04:30:00Z"
  },
  {
    "id": "l01n2h34-8j67-9l01-4m78-3o2h3i4j5k6l7m",
    "user": {
      "id": "user-012",
      "emailHash": "k2n4h7i6j2k7l8m9n0"
    },
    "content": "My pet died today. I'm devastated. They were with me for 15 years and I don't know how to cope with this loss.",
    "emotionTag": "sadness",
    "createdAt": "2024-12-16T03:45:00Z"
  },
  {
    "id": "m12o3i45-9k78-0m12-5n89-4p3i4j5k6l7m8n",
    "user": {
      "id": "user-345",
      "emailHash": "l3o5i8j7k3l8m9n0o1"
    },
    "content": "Someone scratched my new car in the parking lot! I just got it last week! I'm so angry I could scream!",
    "emotionTag": "anger",
    "createdAt": "2024-12-15T02:15:00Z"
  },
  {
    "id": "n23p4j56-0l89-1n23-6o90-5q4j5k6l7m8n9o",
    "user": {
      "id": "user-678",
      "emailHash": "m4p6j9k8l4m9n0o1p2"
    },
    "content": "I'm afraid I'm going to get fired. My boss has been acting strange around me lately and I'm really worried.",
    "emotionTag": "fear",
    "createdAt": "2024-12-14T01:00:00Z"
  },
  {
    "id": "o34q5k67-1m90-2o34-7p01-6r5k6l7m8n9o0p",
    "user": {
      "id": "user-901",
      "emailHash": "n5q7k0l9m5n0o1p2q3"
    },
    "content": "My boyfriend just proposed to me in the middle of a crowded restaurant! I had no idea this was coming! I'm so surprised!",
    "emotionTag": "surprise",
    "createdAt": "2024-12-13T00:30:00Z"
  },
  {
    "id": "p45r6l78-2n01-3p45-8q12-7s6l7m8n9o0p1q",
    "user": {
      "id": "user-234",
      "emailHash": "o6r8l1m0n6o1p2q3r4"
    },
    "content": "The hotel room was absolutely filthy. There were stains on the sheets and the bathroom was covered in mold. Disgusting!",
    "emotionTag": "disgust",
    "createdAt": "2024-12-12T23:45:00Z"
  }
  ]
}