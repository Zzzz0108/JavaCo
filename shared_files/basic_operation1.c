#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
 #define MAX_SIZE 100
 typedef struct stack{
    char arr[MAX_SIZE];
    int top;
 }Stack;
void InitStack(Stack *S){
    S->top=-1;
}
bool StackEmpty(Stack *S){
    return S->top==-1;
}
bool Push(Stack *S,char ch){
    S->top++;
    S->arr[S->top]=ch;
    return true;
}
bool Pop(Stack *S,char *ch){
    *ch=S->arr[S->top];
    S->top--;
    return true;
}
int main(){
    Stack S;
    char x,y;
    InitStack(&S);
    x='c',y='k';
    Push(&S,x);
    Push(&S,'a');
    Push(&S,y);
    Pop(&S,&x);
    Push(&S,'t');
    Push(&S,x);
    Pop(&S,&x);
    Push(&S,'s');
    while(!StackEmpty(&S)){
        Pop(&S,&y);
        printf("%c",y);
    }
    printf("%c\n",x);
}