from django.conf.urls import patterns, include, url
from django.contrib import admin

urlpatterns = patterns('',
    url(r'^$', 'swipperbo.views.init', name='init'),
    url(r'^home/$', 'swipperbo.views.home', name='home'),
    url(r'^home/delete/.*/', 'swipperbo.views.delete', name='delete'),
    url(r'^home/update/.*/', 'swipperbo.views.update', name='update'),
    url(r'^home/addnew/', 'swipperbo.views.addnew', name='addnew'),
    url(r'^admin/', include(admin.site.urls)),
    url(r'^login/$', 'django.contrib.auth.views.login'),
    url(r'^logout/$', 'django.contrib.auth.views.logout', {'next_page': '/'}),

)
