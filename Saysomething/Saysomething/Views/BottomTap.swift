//Bottomtap located on the bottom of the HomePage



import SwiftUI

struct BottomTap: View {
    @State var currentState = "home"
    
    var body: some View {
        HStack{
            Spacer()
            Text("Home").onTapGesture {
                currentState = "home"
            }.foregroundColor(currentState=="home" ?.purple:.gray)
            
            Spacer()
            Text("You AI").onTapGesture {
                currentState = "youAI"
            }.foregroundColor(currentState=="youAI" ?.purple:.gray)
                
            Spacer()
            Image(systemName: "plus.rectangle.fill").onTapGesture {
                currentState = "addPost"
            }.foregroundColor(currentState=="addPost" ?.purple:.gray)
            
            Spacer()
            Text("Message").onTapGesture {
                currentState = "message"
            }.foregroundColor(currentState=="message" ?.purple:.gray)
            
            Spacer()
            Text("Me").onTapGesture {
                currentState = "me"
            }.foregroundColor(currentState=="me" ?.purple:.gray)
            Spacer()
            
        }//HStack end
    }
}

#Preview {
    BottomTap()
}



