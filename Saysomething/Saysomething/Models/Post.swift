//Models/Post
//Defines the data structure of a post 
//Pure Swift (no SwiftUI here).

import Foundation

struct Post: Identifiable {
    let id = UUID() // Let SwiftUI auto-generate UUID for SwiftUI lists
    //let id = String //If backend need a custom ID (e.g. from database)
    
    let resonance: Int  //resonance count
    let view: Int       //view count
    let title: String
    let preImg: String  //image used in post
    let isresonanced: Bool  // Whether current user "resonanced"
}


