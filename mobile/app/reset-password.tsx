import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

const API=process.env.EXPO_PUBLIC_API_URL??'http://10.0.2.2:8080';
const C={ink:'#183A38',muted:'#607674',green:'#286B60',cream:'#F7F8F4',white:'#FFFFFF',line:'#DCE6E1'};

export default function ResetPasswordScreen(){
  const router=useRouter();
  const params=useLocalSearchParams<{token?:string}>();
  const token=Array.isArray(params.token)?params.token[0]:params.token;
  const [password,setPassword]=useState('');
  const [confirmation,setConfirmation]=useState('');
  const [busy,setBusy]=useState(false);
  async function submit(){
    if(!token){Alert.alert('Reset link is incomplete','Request a new password reset link from the sign-in screen.');return;}
    if(password.length<12||password.length>72){Alert.alert('Choose a stronger password','Use 12 to 72 characters.');return;}
    if(password!==confirmation){Alert.alert('Passwords do not match','Enter the same new password in both fields.');return;}
    setBusy(true);
    try{
      const response=await fetch(`${API}/api/auth/password-reset/confirm`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token,newPassword:password})});
      const body=await response.json().catch(()=>({}));
      if(!response.ok)throw new Error(body.detail??body.message??'The reset link could not be used.');
      Alert.alert('Password updated','Sign in with your new password.',[{text:'Continue',onPress:()=>router.replace('/')}]);
    }catch(error){Alert.alert('Could not reset password',error instanceof Error?error.message:'Please try again.');}
    finally{setBusy(false);}
  }
  return <KeyboardAvoidingView style={s.page} behavior={Platform.OS==='ios'?'padding':undefined}><StatusBar style="dark"/><View style={s.card}>
    <Text style={s.eyebrow}>BRAINBOT ACCOUNT</Text><Text style={s.title}>Choose a new password</Text><Text style={s.body}>Reset links are one-time and expire after 15 minutes. Changing your password signs out other sessions.</Text>
    <Text style={s.label}>New password</Text><TextInput accessibilityLabel="New password" value={password} onChangeText={setPassword} secureTextEntry autoCapitalize="none" autoCorrect={false} style={s.input}/>
    <Text style={s.label}>Confirm new password</Text><TextInput accessibilityLabel="Confirm new password" value={confirmation} onChangeText={setConfirmation} secureTextEntry autoCapitalize="none" autoCorrect={false} style={s.input}/>
    <Pressable accessibilityRole="button" disabled={busy} onPress={()=>void submit()} style={[s.button,busy&&{opacity:0.65}]}><Text style={s.buttonText}>{busy?'Updating…':'Update password'}</Text></Pressable>
  </View></KeyboardAvoidingView>;
}

const s=StyleSheet.create({page:{flex:1,backgroundColor:C.cream,justifyContent:'center',padding:24},card:{backgroundColor:C.white,borderColor:C.line,borderWidth:1,borderRadius:20,padding:22},eyebrow:{fontSize:11,letterSpacing:1.2,color:C.green,fontWeight:'700',marginBottom:10},title:{fontSize:27,lineHeight:33,color:C.ink,fontWeight:'700',marginBottom:7},body:{fontSize:14,lineHeight:21,color:C.muted,marginBottom:18},label:{fontSize:13,fontWeight:'600',color:C.ink,marginBottom:7,marginTop:12},input:{height:50,borderRadius:12,borderWidth:1,borderColor:C.line,paddingHorizontal:13,color:C.ink,fontSize:16},button:{height:52,borderRadius:14,backgroundColor:C.green,alignItems:'center',justifyContent:'center',marginTop:22},buttonText:{fontSize:15,fontWeight:'700',color:C.white}});
