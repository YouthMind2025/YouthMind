//This is the post that shown on the home page.
//This version is currently just a basic preview structure of each post
//Not yet connect with Post.swift


import SwiftUI

struct PostPreview: View {
    var resonance: Int
    var view: Int
    var title: String
    var preImg: String

    var body: some View {
        VStack {
            HStack {
                Image(systemName: "heart")
                Text("\(resonance) Resonance")
                VStack {
                    Image(systemName: "eye")
                    Text("\(resonance)")
                }
            }
            Spacer()
            Text(title)
                .font(.title)
                .fontWeight(.semibold)
            Image(preImg) // Preview image
                .resizable()
                .frame(width: 150, height: 150)

            Spacer()
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(10)
    }
}


//Use in future when connect with Post.swift
//struct PostPreview: View {
//    let post: Post
//
//    var body: some View {
//        VStack {
//            HStack(alignment: .center){
//                Image(systemName: "heart")
//                Text("\(post.resonance) Resonance")
//                VStack {
//                    Image(systemName: "eye")
//                    Text("\(post.view)")  // Fixed: Convert Int to String
//                }
//            }
//            Text(post.title)
//            Image(post.preImg) // Preview image
//                .resizable()
//                .frame(width: 150, height: 150)
//
//            Spacer()
//        }
//        .padding()
//        .background(Color.gray.opacity(0.1))
//        .cornerRadius(10)  // Fixed: Corrected spelling from 'cornerRadius'
//    }
//}


