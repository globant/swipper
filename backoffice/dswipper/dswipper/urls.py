from django.conf.urls import patterns, include, url
from django.contrib import admin

urlpatterns = patterns('',
    # Examples:
    url(r'^$', 'swipperbo.views.home', name='home'),
    url(r'^delete/.*/', 'swipperbo.views.delete', name='delete'),
    url(r'^update/.*/', 'swipperbo.views.update', name='update'),
    url(r'^addnew/', 'swipperbo.views.addnew', name='addnew'),
    #url(r'^country/(?P<cc>[a-z]+)/', 'swipperbo.views.country', name='country'),
    # url(r'^blog/', include('blog.urls')),
    #url(r'',),
    url(r'^admin/', include(admin.site.urls)),
)
