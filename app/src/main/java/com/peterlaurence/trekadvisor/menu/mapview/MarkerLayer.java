package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerCallout;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerGrab;
import com.peterlaurence.trekadvisor.menu.mapview.components.MovableMarker;
import com.peterlaurence.trekadvisor.menu.tools.MarkerTouchMoveListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.markers.MarkerLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * All {@link MovableMarker} and {@link MarkerCallout} are managed here. <br>
 * This object is intended to work along with a {@link MapViewFragment}.
 *
 * @author peterLaurence on 09/04/17.
 */
class MarkerLayer implements MapLoader.MapMarkerUpdateListener {
    List<MarkerGson.Marker> mMarkers;
    private MapViewFragment mMapViewFragment;
    private TileView mTileView;
    private Map mMap;
    private MovableMarker mCurrentMovableMarker;


    MarkerLayer(MapViewFragment mapViewFragment) {
        mMapViewFragment = mapViewFragment;
        MapLoader.getInstance().addMapMarkerUpdateListener(this);
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link MarkerTouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private static MarkerGrab attachMarkerGrab(final MovableMarker movableMarker, TileView tileView, Context context) {
        /* Add a view as background, to move easily the marker */
        MarkerTouchMoveListener.MarkerMoveCallback markerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void moveMarker(TileView tileView, View view, double x, double y) {
                tileView.moveMarker(view, x, y);
                tileView.moveMarker(movableMarker, x, y);
                movableMarker.setRelativeX(x);
                movableMarker.setRelativeY(y);
            }
        };

        MarkerGrab markerGrab = new MarkerGrab(context);
        markerGrab.setOnTouchListener(new MarkerTouchMoveListener(tileView, markerMoveCallback));
        tileView.addMarker(markerGrab, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f);
        markerGrab.morphIn();

        return markerGrab;
    }

    MarkerGson.Marker getCurrentMarker() {
        return mCurrentMovableMarker.getMarker();
    }

    private void setCurrentMarker(MovableMarker movableMarker) {
        mCurrentMovableMarker = movableMarker;
    }

    @Override
    public void onMapMarkerUpdate() {
        drawMarkers();
    }

    void setTileView(TileView tileView) {
        mTileView = tileView;

        mTileView.setMarkerTapListener(new MarkerLayout.MarkerTapListener() {
            @Override
            public void onMarkerTap(View view, int x, int y) {
                if (view instanceof MovableMarker) {
                    MovableMarker movableMarker = (MovableMarker) view;

                    /* Prepare the callout */
                    MarkerCallout markerCallout = new MarkerCallout(mMapViewFragment.getContext());
                    markerCallout.setMoveAction(new MorphMarkerRunnable(movableMarker, markerCallout,
                            mTileView, mMapViewFragment.getContext(), mMap));
                    markerCallout.setEditAction(new EditMarkerRunnable(movableMarker, MarkerLayer.this,
                            markerCallout, mTileView,
                            (MapViewFragment.RequestManageMarkerListener) mMapViewFragment.getActivity()));
                    MarkerGson.Marker marker = movableMarker.getMarker();
                    markerCallout.setTitle(marker.name);
                    markerCallout.setSubTitle(marker.lat, marker.lon);

                    mTileView.addCallout(markerCallout, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -1.2f);
                    markerCallout.transitionIn();
                }
            }
        });
    }

    void setMap(Map map) {
        mMap = map;

        /* Update the ui accordingly */
        init();
    }

    /**
     * Triggers the fetch of the map's markers and their drawing on the {@link TileView}. If this is
     * the first time this method is called for this map, the markers aren't defined and the
     * {@link MapLoader} will get them in an asynctask. Otherwise, we can draw them immediately.<br>
     * This must be called when the {@link MapViewFragment} is ready to update its UI.
     */
    private void init() {
        if (mMap.areMarkersDefined()) {
            drawMarkers();
        } else {
            MapLoader.getInstance().getMarkersForMap(mMap);
        }
    }

    private void drawMarkers() {
        mMarkers = mMap.getMarkers();

        for (MarkerGson.Marker marker : mMarkers) {
            MovableMarker movableMarker = new MovableMarker(mMapViewFragment.getContext(), true, marker);
            if (mMap.getProjection() == null) {
                movableMarker.setRelativeX(marker.lon);
                movableMarker.setRelativeY(marker.lat);
            } else {
                movableMarker.setRelativeX(marker.proj_x);
                movableMarker.setRelativeY(marker.proj_y);
            }
            movableMarker.initStatic();

            mTileView.addMarker(movableMarker, movableMarker.getRelativeX(),
                    movableMarker.getRelativeY(), -0.5f, -0.5f);
        }
    }

    /**
     * Add a {@link MovableMarker} to the center of the {@link TileView}.
     */
    void addNewMarker() {
        /* Calculate the relative coordinates of the center of the screen */
        int x = mTileView.getScrollX() + mTileView.getWidth() / 2 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 2 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        final double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        final double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        final MovableMarker movableMarker;
        Context context = mMapViewFragment.getContext();

        /* Create a new marker and add it to the map */
        MarkerGson.Marker newMarker = new MarkerGson.Marker();

        if (mMap.getProjection() == null) {
            newMarker.lat = relativeY;
            newMarker.lon = relativeX;
        } else {
            newMarker.proj_x = relativeX;
            newMarker.proj_y = relativeY;
            double[] wgs84Coords;
            wgs84Coords = mMap.getProjection().undoProjection(relativeX, relativeY);
            if (wgs84Coords != null) {
                newMarker.lon = wgs84Coords[0];
                newMarker.lat = wgs84Coords[1];
            }
        }

        /* Create the corresponding view */
        movableMarker = new MovableMarker(context, false, newMarker);
        movableMarker.setRelativeX(relativeX);
        movableMarker.setRelativeY(relativeY);
        movableMarker.initRounded();

        if (mMap != null) {
            mMap.addMarker(newMarker);
        }

        /* Easily move the marker */
        MarkerGrab markerGrab = attachMarkerGrab(movableMarker, mTileView, mMapViewFragment.getContext());

        movableMarker.setOnClickListener(new MovableMarkerClickListener(movableMarker, markerGrab,
                mTileView, mMap));

        mTileView.addMarker(movableMarker, relativeX, relativeY, -0.5f, -0.5f);
    }

    /**
     * The {@link MarkerGson.Marker} of the {@code mCurrentMovableMarker} has changed. <br>
     * Updates the view.
     */
    void updateCurrentMarker() {
        if (mMap.getProjection() == null) {
            mCurrentMovableMarker.setRelativeX(mCurrentMovableMarker.getMarker().lon);
            mCurrentMovableMarker.setRelativeY(mCurrentMovableMarker.getMarker().lat);
        } else {
            mCurrentMovableMarker.setRelativeX(mCurrentMovableMarker.getMarker().proj_x);
            mCurrentMovableMarker.setRelativeY(mCurrentMovableMarker.getMarker().proj_y);
        }

        mTileView.moveMarker(mCurrentMovableMarker, mCurrentMovableMarker.getRelativeX(),
                mCurrentMovableMarker.getRelativeY());
    }

    /**
     * This listener is only set on a {@link MovableMarker} when it is in its dynamic form (e.g it
     * can be moved). <br>
     * So it does the following :
     * <ul>
     * <li>Morph the {@link MovableMarker} into its static form</li>
     * <li>Animate out and remove the {@link MarkerGrab} which help the user to move the
     * {@link MovableMarker}</li>
     * <li>Update the {@link MarkerGson.Marker} associated with the relative coordinates of the
     * {@link MovableMarker}. Depending on the {@link Map} using a projection or not, those
     * relative coordinates are wgs84 or projected values.</li>
     * </ul>
     */
    private static class MovableMarkerClickListener implements View.OnClickListener {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerGrab> mMarkerGrabWeakReference;
        private TileView mTileView;
        private Map mMap;

        MovableMarkerClickListener(MovableMarker movableMarker, MarkerGrab markerGrab,
                                   TileView tileView, Map map) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerGrabWeakReference = new WeakReference<>(markerGrab);
            mTileView = tileView;
            mMap = map;
        }

        @Override
        public void onClick(View v) {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();
            if (movableMarker != null) {
                movableMarker.morphToStaticForm();

                /* After the morph, the marker should not consume touch events */
                movableMarker.setClickable(false);

                final MarkerGrab markerGrab = mMarkerGrabWeakReference.get();
                markerGrab.morphOut(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        super.onAnimationEnd(drawable);
                        if (markerGrab != null) {
                            mTileView.removeMarker(markerGrab);
                        }
                    }
                });

                /* The view has been moved, update the associated model object */
                MarkerGson.Marker marker = movableMarker.getMarker();
                if (mMap.getProjection() == null) {
                    marker.lon = movableMarker.getRelativeX();
                    marker.lat = movableMarker.getRelativeY();
                } else {
                    marker.proj_x = movableMarker.getRelativeX();
                    marker.proj_y = movableMarker.getRelativeY();
                    double[] wgs84Coords;
                    wgs84Coords = mMap.getProjection().undoProjection(marker.proj_x, marker.proj_y);
                    if (wgs84Coords != null) {
                        marker.lon = wgs84Coords[0];
                        marker.lat = wgs84Coords[1];
                    }
                }

                /* Save the changes on the markers.json file */
                MapLoader.getInstance().saveMarkers(mMap);
            }
        }
    }

    /**
     * This {@link Runnable} is called when an external component requests a {@link MovableMarker} to
     * morph into the dynamic shape. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class MorphMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private TileView mTileView;
        private Context mContext;
        private Map mMap;

        MorphMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, TileView tileView,
                            Context context, Map map) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mTileView = tileView;
            mContext = context;
            mMap = map;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                movableMarker.morphToDynamicForm();

                /* Easily move the marker */
                MarkerGrab markerGrab = attachMarkerGrab(movableMarker, mTileView, mContext);
                movableMarker.setOnClickListener(new MovableMarkerClickListener(movableMarker,
                        markerGrab, mTileView, mMap));

                /* Use a trick to bring the marker to the foreground */
                mTileView.removeMarker(movableMarker);
                mTileView.addMarker(movableMarker, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f);
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                mTileView.removeCallout(markerCallout);
            }
        }
    }

    /**
     * This {@link Runnable} is called when an external component requests a {@link MovableMarker} to
     * be edited. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class EditMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerLayer> mMarkerLayerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private TileView mTileView;
        private WeakReference<MapViewFragment.RequestManageMarkerListener> mListenerWeakRef;

        EditMarkerRunnable(MovableMarker movableMarker, MarkerLayer markerLayer,
                           MarkerCallout markerCallout, TileView tileView,
                           MapViewFragment.RequestManageMarkerListener listener) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerLayerWeakReference = new WeakReference<>(markerLayer);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mTileView = tileView;
            mListenerWeakRef = new WeakReference<>(listener);
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                if (mListenerWeakRef != null) {
                    MapViewFragment.RequestManageMarkerListener listener = mListenerWeakRef.get();
                    if (listener != null) {
                        MarkerLayer markerLayer = mMarkerLayerWeakReference.get();
                        if (markerLayer != null) {
                            markerLayer.setCurrentMarker(movableMarker);
                        }

                        listener.onRequestManageMarker(movableMarker.getMarker());
                    }
                }
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                mTileView.removeCallout(markerCallout);
            }
        }
    }
}
