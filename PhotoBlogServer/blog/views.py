from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone
from .models import Post, ApiUser, GymMachine
from .forms import PostForm
from rest_framework import viewsets
from .serializers import PostSerializer, GymMachineSerializer
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.status import HTTP_400_BAD_REQUEST, HTTP_401_UNAUTHORIZED, HTTP_200_OK
from rest_framework.authtoken.models import Token


def post_list(request):
    posts = Post.objects.filter(published_date__lte=timezone.now()).order_by('published_date')
    return render(request, 'blog/post_list.html', {'posts': posts})

def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})

def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        form = PostForm(request.POST, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})

@api_view(['POST'])
@permission_classes([AllowAny])
def login(request):
    """
    Authenticate user with security_key and return token
    """
    security_key = request.data.get('security_key')

    if not security_key:
        return Response(
            {'error': 'security_key required'},
            status=HTTP_400_BAD_REQUEST
        )

    try:
        api_user = ApiUser.objects.get(
            security_key=security_key,
            is_active=True
        )
        token, _ = Token.objects.get_or_create(user=api_user.user)
        return Response({
            'token': token.key,
            'name': api_user.name
        }, status=HTTP_200_OK)
    except ApiUser.DoesNotExist:
        return Response(
            {'error': 'Invalid security key'},
            status=HTTP_401_UNAUTHORIZED
        )

class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def perform_create(self, serializer):
        # 인증된 사용자를 author로 자동 설정하고 published_date도 설정
        serializer.save(author=self.request.user, published_date=timezone.now())


class GymMachineViewSet(viewsets.ModelViewSet):
    """운동기구 ViewSet"""
    queryset = GymMachine.objects.filter(is_active=True)
    serializer_class = GymMachineSerializer
    permission_classes = [IsAuthenticated]